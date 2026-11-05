package com.andorid.ninmods.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.andorid.ninmods.data.SessionManager
import com.andorid.ninmods.databinding.FragmentProfileBinding
import com.andorid.ninmods.ui.login.LoginActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        binding.tvProfileName.text = sessionManager.userName ?: "NinMods User"
        binding.tvProfileEmail.text = sessionManager.userEmail ?: "user@ninmods.dev"
        binding.tvProfileSessionToken.text = "Token: ${sessionManager.authToken ?: "Verified"}"

        binding.btnProfileLogout.setOnClickListener {
            sessionManager.logout()
            Toast.makeText(requireContext(), "Signed out. Session locked.", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
