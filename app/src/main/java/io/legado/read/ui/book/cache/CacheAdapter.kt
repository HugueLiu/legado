package io.legado.read.ui.book.cache

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import io.legado.read.R
import io.legado.read.base.adapter.ItemViewHolder
import io.legado.read.base.adapter.RecyclerAdapter
import io.legado.read.data.entities.Book
import io.legado.read.databinding.ItemDownloadBinding
import io.legado.read.help.book.isLocal
import io.legado.read.model.CacheBook
import io.legado.read.utils.gone
import io.legado.read.utils.visible

class CacheAdapter(context: Context, private val callBack: CallBack) :
    RecyclerAdapter<Book, ItemDownloadBinding>(context) {



    override fun getViewBinding(parent: ViewGroup): ItemDownloadBinding {
        return ItemDownloadBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDownloadBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                tvName.text = item.name
                tvAuthor.text = context.getString(R.string.author_show, item.getRealAuthor())
                if (item.isLocal) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cs = callBack.cacheChapters[item.bookUrl]
                    if (cs == null) {
                        tvDownload.setText(R.string.loading)
                    } else {
                        tvDownload.text =
                            context.getString(
                                R.string.download_count,
                                cs.size,
                                item.totalChapterNum
                            )
                    }
                }
            } else {
                if (item.isLocal) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cacheSize = callBack.cacheChapters[item.bookUrl]?.size ?: 0
                    tvDownload.text =
                        context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
                }
            }
            upDownloadIv(ivDownload, item)
            upExportInfo(tvMsg, progressExport, item)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemDownloadBinding) {
        binding.run {
            ivDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let { book ->
                    CacheBook.cacheBookMap[book.bookUrl]?.let {
                        if (!it.isStop()) {
                            CacheBook.remove(context, book.bookUrl)
                        } else {
                            CacheBook.start(context, book, 0, book.totalChapterNum)
                        }
                    } ?: let {
                        CacheBook.start(context, book, 0, book.totalChapterNum)
                    }
                }
            }
            tvExport.setOnClickListener {
                callBack.export(holder.layoutPosition)
            }
        }
    }

    private fun upDownloadIv(iv: ImageView, book: Book) {
        if (book.isLocal) {
            iv.gone()
        } else {
            iv.visible()
            CacheBook.cacheBookMap[book.bookUrl]?.let {
                if (!it.isStop()) {
                    iv.setImageResource(R.drawable.ic_stop_black_24dp)
                } else {
                    iv.setImageResource(R.drawable.ic_play_24dp)
                }
            } ?: let {
                iv.setImageResource(R.drawable.ic_play_24dp)
            }
        }
    }

    private fun upExportInfo(msgView: TextView, progressView: ProgressBar, book: Book) {
        val msg = callBack.exportMsg(book.bookUrl)
        if (msg != null) {
            msgView.text = msg
            msgView.visible()
            progressView.gone()
            return
        }
        msgView.gone()
        val progress = callBack.exportProgress(book.bookUrl)
        if (progress != null) {
            progressView.max = book.totalChapterNum
            progressView.progress = progress
            progressView.visible()
            return
        }
        progressView.gone()
    }

    interface CallBack {
        val cacheChapters: HashMap<String, HashSet<String>>
        fun export(position: Int)
        fun exportProgress(bookUrl: String): Int?
        fun exportMsg(bookUrl: String): String?
    }
}