package com.aisportspt.app.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.aisportspt.app.MainActivity
import com.aisportspt.app.R
import com.aisportspt.app.databinding.FragmentAiCoachBinding
import com.aisportspt.app.ui.adapters.FeedbackAdapter
import com.aisportspt.app.model.AIPoseFeedback
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AiCoachFragment : Fragment() {

    private var _binding: FragmentAiCoachBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    
    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackList = mutableListOf<AIPoseFeedback>()
    
    private var isAnalyzing = false
    private var selectedSport = "골프"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCoachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        
        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupUI() {
        setupSportSpinner()
        setupFeedbackRecyclerView()
        setupButtons()
    }

    private fun setupSportSpinner() {
        val viewModel = (requireActivity() as MainActivity).getViewModel()
        
        viewModel.sports.observe(viewLifecycleOwner, Observer { sports ->
            val sportNames = sports.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sportNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSport.adapter = adapter
        })
    }

    private fun setupFeedbackRecyclerView() {
        feedbackAdapter = FeedbackAdapter()
        
//        binding.recyclerViewFeedback?.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = feedbackAdapter
//        }
    }

    private fun setupButtons() {
        binding.btnStartAnalysis.setOnClickListener {
            if (isAnalyzing) {
                stopAnalysis()
            } else {
                startAnalysis()
            }
        }
        
        binding.btnReset.setOnClickListener {
            resetAnalysis()
        }
        
        binding.btnFlipCamera.setOnClickListener {
            flipCamera()
        }
    }

    private fun observeViewModel() {
        // Observe any additional data from ViewModel if needed
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            
            imageAnalyzer = ImageAnalysis.Builder().build().also {
                // TODO: Set up ML Kit pose detection analyzer
                // it.setAnalyzer(cameraExecutor, PoseAnalyzer())
            }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                // Handle error
            }
            
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startAnalysis() {
        isAnalyzing = true
        
        // Update UI
        binding.btnStartAnalysis.text = "분석 중..."
        binding.btnStartAnalysis.isEnabled = false
        binding.aiGuideArea.visibility = View.VISIBLE
        binding.phaseIndicator.visibility = View.VISIBLE
        binding.recordingIndicator.visibility = View.VISIBLE
        
        // Show empty feedback initially
        binding.emptyFeedbackLayout.visibility = View.GONE
//        binding.recyclerViewFeedback?.visibility = View.VISIBLE
        
        // Start pose analysis (mock implementation)
        startMockAnalysis()
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        
        // Update UI
        binding.btnStartAnalysis.text = "분석 시작"
        binding.btnStartAnalysis.isEnabled = true
        binding.recordingIndicator.visibility = View.GONE
    }

    private fun resetAnalysis() {
        stopAnalysis()
        
        // Reset UI
        binding.aiGuideArea.visibility = View.GONE
        binding.phaseIndicator.visibility = View.GONE
        binding.textScore.text = "0%"
        binding.progressScore.progress = 0
        binding.textAccuracyPercent.text = "0%"
        
        // Clear feedback
        feedbackList.clear()
        feedbackAdapter.updateFeedback(feedbackList)
        
        binding.emptyFeedbackLayout.visibility = View.VISIBLE
//        binding.recyclerViewFeedback?.visibility = View.GONE
    }

    private fun flipCamera() {
        // TODO: Implement camera flip functionality
        // This would require rebuilding the camera with front/back selector
    }

    private fun startMockAnalysis() {
        // Mock AI analysis with sample feedback
        val sampleFeedback = listOf(
            AIPoseFeedback(
                message = "좋은 자세입니다! 백스윙 각도가 완벽합니다.",
                confidence = 92,
                type = "good",
                timestamp = System.currentTimeMillis()
            ),
            AIPoseFeedback(
                message = "무릎을 조금 더 구부려보세요.",
                confidence = 88,
                type = "warning",
                timestamp = System.currentTimeMillis() - 2000
            ),
            AIPoseFeedback(
                message = "상체의 회전이 훌륭합니다!",
                confidence = 95,
                type = "good",
                timestamp = System.currentTimeMillis() - 4000
            )
        )
        
        feedbackList.addAll(sampleFeedback)
        feedbackAdapter.updateFeedback(feedbackList)
        
        // Update UI with mock data
        updateAnalysisUI(85, "백스윙")
    }

    private fun updateAnalysisUI(score: Int, phase: String) {
        binding.textScore.text = "${score}%"
        binding.progressScore.progress = score
        binding.textAccuracyPercent.text = "${score}%"
        binding.textCurrentPhase.text = "현재 단계: $phase"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}