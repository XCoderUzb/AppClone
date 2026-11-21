package uz.appclone

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

/** Kategoriya ichidagi fayllar ro'yxati: rasm, nom va "⋮" menyusi (o'chirish). */
class CategoryActivity : Activity() {

    private lateinit var cat: String
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var list: ListView
    private val adapter = FileAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cat = intent.getStringExtra("cat") ?: run {
            finish()
            return
        }
        setContentView(R.layout.activity_category)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvCatTitle).text = "${Cat.emoji(cat)} $cat"
        loading = findViewById(R.id.loading)
        empty = findViewById(R.id.empty)
        list = findViewById(R.id.list)
        list.adapter = adapter

        load()
    }

    private fun load() {
        loading.visibility = View.VISIBLE
        empty.visibility = View.GONE
        thread {
            try {
                val token = Auth.tokenBlocking(this) ?: throw Exception("Ilovaga qayta kiring")
                val folder = DriveApi.categoryFolder(this, token, cat)
                val files = DriveApi.listFiles(token, folder)
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    adapter.items.clear()
                    adapter.items.addAll(files)
                    adapter.resetAnimation()
                    adapter.notifyDataSetChanged()
                    Prefs.setCount(this, cat, files.size)
                    loading.visibility = View.GONE
                    showEmptyIfNeeded()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    loading.visibility = View.GONE
                    empty.text = "Yuklab bo'lmadi: ${e.message}"
                    empty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showEmptyIfNeeded() {
        if (adapter.items.isEmpty()) {
            empty.text = "Bu yerda hali fayl yo'q"
            empty.visibility = View.VISIBLE
            empty.slideUpIn(0)
        } else {
            empty.visibility = View.GONE
        }
    }

    private fun showMenu(anchor: View, file: DriveApi.DriveFile, row: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, "🗑  O'chirish")
            setOnMenuItemClickListener {
                confirmDelete(file, row)
                true
            }
            show()
        }
    }

    private fun confirmDelete(file: DriveApi.DriveFile, row: View) {
        AlertDialog.Builder(this)
            .setTitle("O'chirish")
            .setMessage("\"${file.name}\" Bazadan o'chiriladi. U Google Drive axlat qutisiga tushadi va 30 kun ichida tiklash mumkin.")
            .setPositiveButton("O'chirish") { _, _ -> delete(file, row) }
            .setNegativeButton("Bekor", null)
            .show()
    }

    private fun delete(file: DriveApi.DriveFile, row: View) {
        thread {
            try {
                val token = Auth.tokenBlocking(this) ?: throw Exception("Ilovaga qayta kiring")
                DriveApi.trash(token, file.id)
                runOnUiThread {
                    row.animate().alpha(0f).translationX(300f).setDuration(250).withEndAction {
                        adapter.items.remove(file)
                        adapter.notifyDataSetChanged()
                        Prefs.setCount(this, cat, adapter.items.size)
                        Prefs.setUsedBytes(this, maxOf(0L, Prefs.usedBytes(this) - file.size))
                        showEmptyIfNeeded()
                    }.start()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "O'chirib bo'lmadi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private inner class FileAdapter : BaseAdapter() {
        val items = mutableListOf<DriveApi.DriveFile>()
        private var animatedUpTo = -1

        fun resetAnimation() {
            animatedUpTo = -1
        }

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView
                ?: LayoutInflater.from(this@CategoryActivity).inflate(R.layout.item_file, parent, false)
            val f = items[position]

            v.animate().cancel()
            v.alpha = 1f
            v.translationX = 0f
            v.translationY = 0f

            val thumb = v.findViewById<ImageView>(R.id.fileThumb)
            thumb.makeRounded(12 * resources.displayMetrics.density)
            thumb.setImageBitmap(Thumbs.placeholder(Cat.emoji(Cat.of(f.mime))))
            thumb.tag = null
            f.thumb?.let { Thumbs.load(it, thumb, this@CategoryActivity) }

            v.findViewById<TextView>(R.id.fileName).text = f.name
            v.findViewById<TextView>(R.id.fileInfo).text =
                "${formatSize(f.size)}  •  ${f.created.take(10)}"
            v.findViewById<View>(R.id.fileMenu).setOnClickListener { showMenu(it, f, v) }

            if (position > animatedUpTo && position < 15) {
                animatedUpTo = position
                v.slideUpIn(position * 40L)
            }
            return v
        }
    }
}
