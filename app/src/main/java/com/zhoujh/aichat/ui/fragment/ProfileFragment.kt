package com.zhoujh.aichat.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.zhoujh.aichat.databinding.FragmentProfileBinding
import com.zhoujh.aichat.ui.activity.ConfigActivity
import com.zhoujh.aichat.ui.activity.ImgTestActivity
import com.zhoujh.aichat.ui.activity.TextTestActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        binding.btnSetting.setOnClickListener {
            startActivity(Intent(requireContext(), ConfigActivity::class.java))
        }
        binding.btnTextTest.setOnClickListener {
            startActivity(Intent(requireContext(), TextTestActivity::class.java))
        }
        binding.btnImgTest.setOnClickListener {
            startActivity(Intent(requireContext(), ImgTestActivity::class.java))
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}