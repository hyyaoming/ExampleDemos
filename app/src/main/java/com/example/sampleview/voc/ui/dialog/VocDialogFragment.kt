package com.example.sampleview.voc.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.chad.library.adapter.base.BaseQuickAdapter
import com.example.sampleview.Utils
import com.example.sampleview.databinding.DialogFragmentVocBinding
import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.ui.VocItem
import com.example.sampleview.voc.ui.adapter.VocAdapter
import com.example.sampleview.voc.ui.decoration.VocItemDecoration

/**
 * 问卷对话框 Fragment，用于展示问卷问题并收集用户答案。
 *
 * 特性：
 * - 支持提交答案或取消问卷。
 * - 根据问题选项动态显示多选项列表。
 * - 内部使用 RecyclerView 显示问题选项。
 * - 支持保存状态，防止配置变更丢失数据。
 */
class VocDialogFragment : DialogFragment() {

    /** 用户提交答案回调 */
    var onSubmitListener: ((Answer) -> Unit)? = null

    /** 用户取消问卷回调 */
    var onCancelListener: (() -> Unit)? = null

    /** 当前用户填写的答案 */
    private var answer = Answer()

    /** 当前问卷问题 */
    private var question: Question? = null

    /** 多选项列表 */
    private val vocItems: ArrayList<VocItem> = arrayListOf()

    /** ViewBinding */
    private lateinit var vocBinding: DialogFragmentVocBinding

    companion object {
        /**
         * 创建新的问卷对话框实例
         *
         * @param questions 当前问卷问题 [Question]
         * @return VocDialogFragment 实例
         */
        fun newInstance(questions: Question): VocDialogFragment {
            val fragment = VocDialogFragment()
            fragment.question = questions
            return fragment
        }
    }

    /**
     * 保存状态，防止配置变更丢失数据
     *
     * @param outState 保存状态的 Bundle
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("answer", answer)
        outState.putParcelableArrayList("vocItems", vocItems)
        outState.putParcelable("question", question)
    }

    /**
     * Fragment 创建完成后初始化视图和事件
     *
     * @param view 根视图
     * @param savedInstanceState 保存状态 Bundle
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 控制是否可关闭
        dialog?.setCancelable(question?.canClose != false)
        dialog?.setCanceledOnTouchOutside(question?.canClose != false)

        // 恢复状态
        if (savedInstanceState != null) {
            answer = savedInstanceState.getParcelable("answer") ?: Answer()
            vocItems.clear()
            vocItems.addAll(savedInstanceState.getParcelableArrayList("vocItems") ?: emptyList())
            question = savedInstanceState.getParcelable("question") ?: question
        }

        // 初始化标题
        vocBinding.tvTitle.text = question?.title
        vocBinding.tvSubTitle.text = question?.subTitle

        // 监听其他原因输入
        vocBinding.editOtherReason.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                answer.otherReason = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 提交按钮事件
        vocBinding.btnSubmit.setOnClickListener {
            onSubmitListener?.invoke(answer)
        }

        // 关闭按钮事件
        vocBinding.ivClose.setOnClickListener {
            onCancelListener?.invoke()
            dismiss()
        }

        // 选择没选择
        vocBinding.tvNoProblem.setOnClickListener {
            answer.hasProblem = false
            vocBinding.btnSubmit.isEnabled = true
        }

        // 选择有问题
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogFragmentVocBinding.inflate(inflater, container, false).also { vocBinding = it }.root
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