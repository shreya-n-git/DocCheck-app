package com.doccheck.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.doccheck.app.R
import com.doccheck.app.data.model.Source
import com.doccheck.app.databinding.BottomSheetSourceBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SourceBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSourceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = arguments?.getString(ARG_TITLE).orEmpty()
        val page = arguments?.getInt(ARG_PAGE) ?: 1
        val snippet = arguments?.getString(ARG_SNIPPET).orEmpty()

        binding.tvSourceTitlePage.text = getString(R.string.chat_source_sheet_header_fmt, title, page)
        binding.tvSourceSnippet.text = snippet
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_PAGE = "arg_page"
        private const val ARG_SNIPPET = "arg_snippet"

        fun newInstance(source: Source): SourceBottomSheetDialogFragment {
            return SourceBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, source.title)
                    putInt(ARG_PAGE, source.page)
                    putString(ARG_SNIPPET, source.snippet)
                }
            }
        }
    }
}

