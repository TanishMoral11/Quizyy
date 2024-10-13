package com.example.quizyy

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var explanationTextView: TextView
    private lateinit var takeQuizButton: Button
    private lateinit var uploadAnimation: LottieAnimationView
    private lateinit var sharedPreferences: SharedPreferences  // Initialize SharedPreferences
    private lateinit var profileIcon: View
    private lateinit var auth: FirebaseAuth



    private val apiKey = "AIzaSyBqbEhASS_GfmQP7WC9FjKKYf8QMZt8yKw"
    private val geminiModel = GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey)
    private var pdfContent: String = ""
    private var isGeneratingQuiz = false
    private lateinit var maincontent: Content
    private lateinit var loadingDialog: AlertDialog

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { processFile(it) }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        uploadButton = findViewById(R.id.uploadButton)
        explanationTextView = findViewById(R.id.explanationTextView)
        takeQuizButton = findViewById(R.id.takeQuizButton)
        uploadAnimation = findViewById(R.id.uploadAnimation)
        profileIcon = findViewById(R.id.profileIcon)  // Initialize profileIcon

        uploadButton.setOnClickListener {
            openFilePicker()
        }

        takeQuizButton.setOnClickListener {
            generateQuizAndNavigate()
        }

        takeQuizButton.visibility = View.GONE



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showLoading(show: Boolean) {
        uploadAnimation.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setLoggedIn(loggedIn: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("logged_in", loggedIn)
        editor.apply()
    }

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this, R.style.CenterDialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.5f)
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        loadingDialog.show()
    }

    private fun dismissLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg", "image/png"))
        }
        getContent.launch(intent)
    }

    private fun processFile(uri: Uri) {
        pdfContent = ""
        explanationTextView.text = ""
        takeQuizButton.visibility = View.GONE
        isGeneratingQuiz = false

        val mimeType = contentResolver.getType(uri)
        when {
            mimeType == "application/pdf" -> extractTextFromPdf(uri)
            mimeType?.startsWith("image/") == true -> processImage(uri)
            else -> explanationTextView.text = "Unsupported file type"
        }
    }

    private fun extractTextFromPdf(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val pdfReader = PdfReader(inputStream)
                val pdfDocument = PdfDocument(pdfReader)
                val pageNum = pdfDocument.numberOfPages.coerceAtMost(1)
                pdfContent = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNum))
                pdfDocument.close()

                Log.d("QuizDebug", "PDF Content: $pdfContent")

                analyzeWithGemini(pdfContent)
            }
        } catch (e: IOException) {
            explanationTextView.text = "Error extracting text from PDF: ${e.message}"
        }
    }

    private fun processImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    analyzeWithGemini(bitmap)
                } else {
                    Log.e("ImageProcessing", "Failed to decode bitmap")
                    explanationTextView.text = "Error: Unable to process the image"
                }
            } ?: run {
                Log.e("ImageProcessing", "Failed to open input stream")
                explanationTextView.text = "Error: Unable to open the image file"
            }
        } catch (e: IOException) {
            Log.e("ImageProcessing", "Error processing image", e)
            explanationTextView.text = "Error processing image: ${e.message}"
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Unexpected error", e)
            explanationTextView.text = "Unexpected error: ${e.message}"
        }
    }

    private fun analyzeWithGemini(input: Any) {
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                maincontent = when (input) {
                    is String -> content {
                        text("Explain the following content in short and very easy way like 14 year old kid:\n\n$input")
                    }
                    is Bitmap -> content {
                        image(input)
                        text("Explain the content of this image in short and very easy way like 14 year old kid:")
                    }
                    else -> throw IllegalArgumentException("Unsupported input type")
                }

                val response = geminiModel.generateContent(maincontent)
                if (response.text != null) {
                    explanationTextView.text = response.text.toString()
                    explanationTextView.setTextColor(android.graphics.Color.BLACK)
                    takeQuizButton.visibility = View.VISIBLE
                    pdfContent = input.toString()
                } else {
                    Log.e("Gemini", "Empty response from API")
                    explanationTextView.text = "Error: No response from AI model"
                }
            } catch (e: Exception) {
                Log.e("Gemini", "Error analyzing content", e)
                explanationTextView.text = "Error analyzing content: ${e.message}"
                takeQuizButton.visibility = View.GONE
            } finally {
                showLoading(false)
            }
        }
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int = 1024, maxHeight: Int = 1024): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap

        val ratio = Math.min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun generateQuizAndNavigate() {
        if (isGeneratingQuiz) {
            Toast.makeText(this, "Please wait, quiz is generating...", Toast.LENGTH_SHORT).show()
            return
        }

        if (!::maincontent.isInitialized) {
            Toast.makeText(this, "Please analyze a document first", Toast.LENGTH_SHORT).show()
            return
        }

        isGeneratingQuiz = true
        takeQuizButton.isEnabled = false
        showLoadingDialog()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("QuizDebug", "Starting quiz generation")
                val quizPrompt = content {
                    text("Based on the following content, generate 5 multiple-choice questions with 4 options each. Format the response as a JSON array with each question object containing 'question', 'options' (as an array), and 'correctAnswer' (as an index 0-3). Ensure that the correct answer is randomly positioned for each question.\n\nContent: ${explanationTextView.text}")
                }

                Log.d("QuizDebug", "Sending request to Gemini")
                val response = geminiModel.generateContent(quizPrompt)
                val quizJson = response.text?.toString() ?: ""

                Log.d("QuizDebug", "Received response from Gemini: $quizJson")

                if (isValidQuizJson(quizJson)) {
                    dismissLoadingDialog()
                    navigateToQuiz(quizJson)
                } else {
                    Log.e("QuizDebug", "Invalid response from API")
                    Toast.makeText(this@MainActivity, "Invalid quiz format received", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("QuizDebug", "Error generating quiz", e)
                Toast.makeText(this@MainActivity, "Error generating quiz", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingQuiz = false
                takeQuizButton.isEnabled = true
                dismissLoadingDialog()
            }
        }
    }

    private fun isValidQuizJson(json: String): Boolean {
        return try {
            val cleanedJson = json.replace("```json", "").replace("```", "").trim()
            val jsonArray = JSONArray(cleanedJson)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                jsonObject.getString("question")
                val optionsArray = jsonObject.getJSONArray("options")
                if (optionsArray.length() != 4) {
                    return false
                }
                for (j in 0 until optionsArray.length()) {
                    optionsArray.getString(j)
                }
                val correctAnswer = jsonObject.getInt("correctAnswer")
                if (correctAnswer < 0 || correctAnswer > 3) {
                    return false
                }
            }
            true
        } catch (e: JSONException) {
            Log.e("QuizDebug", "Invalid JSON format", e)
            Log.e("QuizDebug", "Received JSON: $json")
            false
        }
    }

    private fun navigateToQuiz(quizJson: String) {
        val intent = Intent(this@MainActivity, QuizActivity::class.java).apply {
            putExtra("QUIZ_JSON", quizJson)
            putExtra("EXPLANATION", explanationTextView.text.toString())
        }
        startActivity(intent)
    }


}
