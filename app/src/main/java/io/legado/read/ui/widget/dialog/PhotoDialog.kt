package io.legado.read.ui.widget.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.request.RequestOptions
import io.legado.read.R
import io.legado.read.base.BaseDialogFragment
import io.legado.read.databinding.DialogPhotoViewBinding
import io.legado.read.help.book.BookHelp
import io.legado.read.help.glide.ImageLoader
import io.legado.read.help.glide.OkHttpModelLoader
import io.legado.read.model.BookCover
import io.legado.read.model.ReadBook
import io.legado.read.ui.book.read.page.provider.ImageProvider
import io.legado.read.utils.setLayout
import io.legado.read.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: return
        arguments.getString("src")?.let { src ->
            ImageProvider.bitmapLruCache.get(src)?.let {
                binding.photoView.setImageBitmap(it)
                return
            }
            val file = ReadBook.book?.let { book ->
                BookHelp.getImage(book, src)
            }
            if (file?.exists() == true) {
                ImageLoader.load(requireContext(), file)
                    .error(R.drawable.image_loading_error)
                    .into(binding.photoView)
            } else {
                ImageLoader.load(requireContext(), src).apply {
                    arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                        apply(
                            RequestOptions().set(
                                OkHttpModelLoader.sourceOriginOption,
                                sourceOrigin
                            )
                        )
                    }
                }.error(BookCover.defaultDrawable)
                    .into(binding.photoView)
            }
        }
    }

}
