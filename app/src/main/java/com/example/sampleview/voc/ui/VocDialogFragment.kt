package com.example.sampleview.voc.ui

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.QuickViewHolder
import com.example.sampleview.R
import com.example.sampleview.Utils
import com.example.sampleview.databinding.DialogFragmentVocBinding
import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import kotlinx.parcelize.Parcelize

class VocDialogFragment : DialogFragment() {
    var onSubmitListener: ((Answer) -> Unit)? = null
    var onCancelListener: (() -> Unit)? = null
    private var answer = Answer()

    private var question: Question? = null
    private val vocItems: ArrayList<VocItem> = arrayListOf()
    private lateinit var vocBinding: DialogFragmentVocBinding

    companion object {
        fun newInstance(questions: Question): VocDialogFragment {
            val fragment = VocDialogFragment()
            fragment.question = questions
            return fragment
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("answer", answer)
        outState.putParcelableArrayList("vocItems", vocItems)
        outState.putParcelable("question", question)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCancelable(question?.canClose != false)
        dialog?.setCanceledOnTouchOutside(question?.canClose != false)
        if (savedInstanceState != null) {
            answer = savedInstanceState.getParcelable("answer") ?: Answer()
            vocItems.clear()
            vocItems.addAll(savedInstanceState.getParcelableArrayList("vocItems") ?: emptyList())
            question = savedInstanceState.getParcelable("question") ?: question
        }
        vocBinding.tvTitle.text = question?.title
        vocBinding.tvSubTitle.text = question?.subTitle
        vocBinding.editOtherReason.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                answer.otherReason = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        vocBinding.btnSubmit.setOnClickListener {
            onSubmitListener?.invoke(answer)
        }
        vocBinding.ivClose.setOnClickListener {
            onCancelListener?.invoke()
            dismiss()
        }
        vocBinding.tvNoProblem.setOnClickListener {
            answer.hasProblem = false
            vocBinding.btnSubmit.isEnabled = true
        }
        vocBinding.tvProblem.setOnClickListener {
            answer.hasProblem = false
            vocBinding.btnSubmit.isEnabled = true
            if (vocBinding.clProblemDetail.isGone && !question?.options.isNullOrEmpty()) {
                if (vocItems.isEmpty()) {
                    question?.options?.map { VocItem(it) }?.apply {
                        vocItems.clear()
                        vocItems.addAll(this)
                    }
                }
                vocBinding.clProblemDetail.visibility = View.VISIBLE
                vocBinding.rvProblem.addItemDecoration(VocItemDecoration(2, Utils.dip2px(requireActivity(), 4)))
                vocBinding.rvProblem.adapter = VocAdapter(vocItems).apply {
                    setOnItemClickListener(object : BaseQuickAdapter.OnItemClickListener<VocItem> {
                        override fun onClick(adapter: BaseQuickAdapter<VocItem, *>, view: View, position: Int) {
                            val vocItem = vocItems[position]
                            if (vocItem.select) {
                                answer.problems.remove(vocItem.string)
                            } else {
                                answer.problems.add(vocItem.string)
                            }
                            vocItem.select = !vocItem.select
                            adapter.notifyItemChanged(position)
                        }
                    })
                }
            }
        }
    }

    @Parcelize
    data class VocItem(
        val string: String,
        var select: Boolean = false,
    ) : Parcelable

    inner class VocItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = state.itemCount
            val column = position % spanCount
            val row = position / spanCount
            val rowCount = (itemCount + spanCount - 1) / spanCount

            // 横向间距：第 1 列没有左间距，最后 1 列没有右间距，中间的才有
            outRect.left = if (column == 0) 0 else spacing
            outRect.right = if (column == spanCount - 1) 0 else spacing

            // 顶部间距：第一行不要
            outRect.top = if (row == 0) 0 else spacing

            // 底部间距：最后一行不要
            outRect.bottom = if (row == rowCount - 1) 0 else spacing
        }
    }

    inner class VocAdapter(vocItems: List<VocItem>) : BaseQuickAdapter<VocItem, QuickViewHolder>(vocItems) {
        override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: VocItem?) {
            item ?: return
            val tvItem = holder.itemView as TextView
            tvItem.text = item.string
            if (item.select) {
                tvItem.setTextColor("#00B14F".toColorInt())
            } else {
                tvItem.setTextColor("#1A1A1A".toColorInt())
            }
        }

        override fun onCreateViewHolder(
            context: Context,
            parent: ViewGroup,
            viewType: Int,
        ): QuickViewHolder {
            return QuickViewHolder(LayoutInflater.from(context).inflate(R.layout.item_voc, parent, false))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogFragmentVocBinding.inflate(inflater, container, false).also {
            vocBinding = it
        }.root
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        window.decorView.setPadding(0, 0, 0, 0)
        window.setDimAmount(0.25f)
        window.setGravity(Gravity.BOTTOM)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setWindowAnimations(-1)
        val layoutParams = window.attributes
        layoutParams.width = resources.displayMetrics.widthPixels
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = layoutParams
    }
}
