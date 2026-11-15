package com.stratatechlabs.obswificams.stream

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stratatechlabs.obswificams.R
import com.stratatechlabs.obswificams.core.StreamFps
import com.stratatechlabs.obswificams.core.StreamQuality
import com.stratatechlabs.obswificams.databinding.SheetWifiShareBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class WifiShareBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetWifiShareBinding? = null
    private val binding get() = _binding!!

    var streamManager: WifiStreamManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetWifiShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val manager = streamManager ?: return

        setupViews(manager)
        observeState(manager)
    }

    private fun setupViews(manager: WifiStreamManager) {
        binding.wifiShareSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val started = manager.startStreaming()
                if (!started) {
                    binding.wifiShareSwitch.isChecked = false
                    Toast.makeText(requireContext(), R.string.wifi_share_no_wifi, Toast.LENGTH_SHORT).show()
                }
            } else {
                manager.stopStreaming()
            }
        }

        binding.btnWifiFlashToggle.setOnClickListener {
            val newState = manager.toggleTorch()
            val hasFlashUnit = manager.hasFlashUnit
            updateFlashButtonState(newState, hasFlashUnit)
        }

        binding.btnCopyBrowserUrl.setOnClickListener {
            copyToClipboard(binding.browserUrlText.text.toString())
        }

        binding.btnOpenBrowser.setOnClickListener {
            val url = binding.browserUrlText.text.toString()
            if (url.startsWith("http")) {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }.onFailure {
                    Toast.makeText(requireContext(), R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnCopyObsUrl.setOnClickListener {
            copyToClipboard(binding.obsUrlText.text.toString())
        }

        binding.btnCopyCleanObsUrl.setOnClickListener {
            copyToClipboard(binding.obsCleanUrlText.text.toString())
        }

        binding.btnCopyAudioUrl.setOnClickListener {
            copyToClipboard(binding.audioUrlText.text.toString())
        }

        binding.qualityChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                R.id.chipQualityAuto in checkedIds -> manager.setQuality(StreamQuality.AUTO)
                R.id.chip4k in checkedIds -> manager.setQuality(StreamQuality.UHD_4K)
                R.id.chip2k in checkedIds -> manager.setQuality(StreamQuality.QHD_2K)
                R.id.chip1080p in checkedIds -> manager.setQuality(StreamQuality.FHD_1080P)
                R.id.chip480p in checkedIds -> manager.setQuality(StreamQuality.SD_480P)
                else -> manager.setQuality(StreamQuality.HD_720P)
            }
        }

        binding.fpsChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val fps = when {
                R.id.chipFps120 in checkedIds -> StreamFps.FPS_120
                R.id.chipFps60 in checkedIds -> StreamFps.FPS_60
                R.id.chipFps30 in checkedIds -> StreamFps.FPS_30
                R.id.chipFps24 in checkedIds -> StreamFps.FPS_24
                R.id.chipFps15 in checkedIds -> StreamFps.FPS_15
                else -> StreamFps.AUTO
            }
            manager.setFps(fps)
        }

        binding.rotationChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val deg = when {
                R.id.chipRot90 in checkedIds -> 90
                R.id.chipRot180 in checkedIds -> 180
                R.id.chipRot270 in checkedIds -> 270
                else -> 0
            }
            manager.setRotation(deg)
        }

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                R.id.chipFilterBluray in checkedIds -> StreamFilter.BLURAY
                R.id.chipFilterTealOrange in checkedIds -> StreamFilter.TEAL_ORANGE
                R.id.chipFilterVivid in checkedIds -> StreamFilter.VIVID
                R.id.chipFilterVintage in checkedIds -> StreamFilter.VINTAGE
                R.id.chipFilterNoir in checkedIds -> StreamFilter.NOIR
                R.id.chipFilterCyberpunk in checkedIds -> StreamFilter.CYBERPUNK
                R.id.chipFilterInvert in checkedIds -> StreamFilter.INVERT
                else -> StreamFilter.NONE
            }
            manager.setFilter(filter)
        }

        binding.keepAwakeSwitch.isChecked = true
        binding.keepAwakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val win = activity?.window ?: return@setOnCheckedChangeListener
            if (isChecked) {
                win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        binding.dimScreenSwitch.setOnCheckedChangeListener { _, isChecked ->
            val win = activity?.window ?: return@setOnCheckedChangeListener
            val lp = win.attributes
            lp.screenBrightness = if (isChecked) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            win.attributes = lp
        }

        binding.autoAdjustSwitch.isChecked = manager.state.value.autoAdjust
        binding.autoAdjustSwitch.setOnCheckedChangeListener { _, isChecked ->
            manager.setAutoAdjust(isChecked)
        }
    }

    private fun observeState(manager: WifiStreamManager) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                manager.state.collect { st ->
                    updateUi(st, manager)
                }
            }
        }
    }

    @android.annotation.SuppressLint("SetTextI18n")
    private fun updateUi(st: WifiStreamState, manager: WifiStreamManager) {
        val hasIp = st.ipAddress != null
        binding.wifiWarningLayout.visibility = if (!hasIp) View.VISIBLE else View.GONE
        binding.wifiActiveLayout.visibility = if (st.isStreaming) View.VISIBLE else View.GONE

        if (binding.wifiShareSwitch.isChecked != st.isStreaming) {
            binding.wifiShareSwitch.isChecked = st.isStreaming
        }

        if (binding.autoAdjustSwitch.isChecked != st.autoAdjust) {
            binding.autoAdjustSwitch.isChecked = st.autoAdjust
        }

        val hasFlashUnit = manager.hasFlashUnit
        updateFlashButtonState(st.isTorchOn, hasFlashUnit)

        if (st.isStreaming && (st.ipAddress != null)) {
            val base = "http://${st.ipAddress}:${st.port}"
            binding.browserUrlText.text = "$base/"
            binding.obsUrlText.text = "$base/video.mjpeg"
            binding.obsCleanUrlText.text = "$base/obs"
            binding.audioUrlText.text = "$base/audio.wav"

            val clientsStr = if (st.clientCount > 0) {
                getString(R.string.wifi_share_clients, st.clientCount)
            } else {
                getString(R.string.wifi_share_waiting)
            }
            val resLabel = if (st.quality == StreamQuality.AUTO) {
                "${st.effectiveQuality.targetWidth}x${st.effectiveQuality.targetHeight} (Auto)"
            } else {
                "${st.effectiveQuality.targetWidth}x${st.effectiveQuality.targetHeight}"
            }
            binding.wifiStatusText.text = getString(R.string.wifi_share_live_chip, "$clientsStr · ${st.fps} FPS · $resLabel")
        }

        val qualityChipId = when (st.quality) {
            StreamQuality.AUTO -> R.id.chipQualityAuto
            StreamQuality.UHD_4K -> R.id.chip4k
            StreamQuality.QHD_2K -> R.id.chip2k
            StreamQuality.FHD_1080P -> R.id.chip1080p
            StreamQuality.SD_480P -> R.id.chip480p
            StreamQuality.HD_720P -> R.id.chip720p
        }
        if (binding.qualityChipGroup.checkedChipId != qualityChipId) {
            binding.qualityChipGroup.check(qualityChipId)
        }

        val fpsChipId = when (st.targetFps) {
            StreamFps.FPS_120 -> R.id.chipFps120
            StreamFps.FPS_60 -> R.id.chipFps60
            StreamFps.FPS_30 -> R.id.chipFps30
            StreamFps.FPS_24 -> R.id.chipFps24
            StreamFps.FPS_15 -> R.id.chipFps15
            StreamFps.AUTO -> R.id.chipFpsAuto
        }
        if (binding.fpsChipGroup.checkedChipId != fpsChipId) {
            binding.fpsChipGroup.check(fpsChipId)
        }

        val targetRotChipId = when (st.rotation % 360) {
            90 -> R.id.chipRot90
            180 -> R.id.chipRot180
            270 -> R.id.chipRot270
            else -> R.id.chipRot0
        }
        if (binding.rotationChipGroup.checkedChipId != targetRotChipId) {
            binding.rotationChipGroup.check(targetRotChipId)
        }

        val targetFilterChipId = when (st.filter) {
            StreamFilter.BLURAY -> R.id.chipFilterBluray
            StreamFilter.TEAL_ORANGE -> R.id.chipFilterTealOrange
            StreamFilter.VIVID -> R.id.chipFilterVivid
            StreamFilter.VINTAGE -> R.id.chipFilterVintage
            StreamFilter.NOIR -> R.id.chipFilterNoir
            StreamFilter.CYBERPUNK -> R.id.chipFilterCyberpunk
            StreamFilter.INVERT -> R.id.chipFilterInvert
            StreamFilter.NONE -> R.id.chipFilterNone
        }
        if (binding.filterChipGroup.checkedChipId != targetFilterChipId) {
            binding.filterChipGroup.check(targetFilterChipId)
        }
    }

    private fun updateFlashButtonState(isTorchOn: Boolean, hasFlashUnit: Boolean) {
        if (!_binding.run { this != null }) return
        val context = context ?: return

        if (!hasFlashUnit) {
            binding.btnWifiFlashToggle.isEnabled = false
            binding.btnWifiFlashToggle.setText(R.string.wifi_share_flash_unavailable)
            binding.btnWifiFlashToggle.setIconResource(R.drawable.ic_flash_off)
            binding.btnWifiFlashToggle.setIconTintResource(R.color.on_surface_variant)
            binding.btnWifiFlashToggle.alpha = 0.5f
            return
        }

        binding.btnWifiFlashToggle.isEnabled = true
        binding.btnWifiFlashToggle.alpha = 1.0f
        if (isTorchOn) {
            binding.btnWifiFlashToggle.setText(R.string.wifi_share_flash_on)
            binding.btnWifiFlashToggle.setIconResource(R.drawable.ic_torch)
            val accentColor = ContextCompat.getColor(context, R.color.accent)
            binding.btnWifiFlashToggle.setIconTintResource(R.color.accent)
            binding.btnWifiFlashToggle.setTextColor(accentColor)
        } else {
            binding.btnWifiFlashToggle.setText(R.string.wifi_share_flash_off)
            binding.btnWifiFlashToggle.setIconResource(R.drawable.ic_flash_off)
            binding.btnWifiFlashToggle.setIconTintResource(R.color.on_surface_variant)
            binding.btnWifiFlashToggle.setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Stream URL", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.wifi_share_copied, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        // Automatically turn off the flashlight when leaving the dashboard so LED does not stay stuck on
        streamManager?.setTorch(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "WifiShareBottomSheet"
        fun newInstance(manager: WifiStreamManager) = WifiShareBottomSheet().apply {
            streamManager = manager
        }
    }
}
