// Package declaration, this is the namespace for the class
package com.example.quizyy

// Import statements bring in Android and other necessary classes and interfaces used in the code
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONException

// QuizActivity class handles the quiz logic and extends AppCompatActivity for compatibility with older Android versions
class QuizActivity : AppCompatActivity() {

    // Declare UI elements like TextViews, Buttons, and ProgressBar
    private lateinit var questionTextView: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var nextButton: Button
    private lateinit var progressTextView: TextView
    private lateinit var progressBar: ProgressBar
    private var totalQuestions: Int = 0

    // JSON array to hold quiz data and variables to manage the current question and score
    private var quizData: JSONArray = JSONArray()
    private var currentQuestionIndex: Int = -1 // Start at -1 so the first question is 0
    private var score: Int = 0

    // Companion object holds constants that can be accessed from anywhere in this class
    companion object {
        private const val SCORE_LOW = 0
        private const val SCORE_MEDIUM = 2
        private const val SCORE_HIGH = 5
    }

    // onCreate method is called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz) // Set the layout for this activity

        // Initialize UI elements by finding them by their ID in the layout
        progressTextView = findViewById(R.id.progressTextView)
        progressBar = findViewById(R.id.progressBar)
        questionTextView = findViewById(R.id.questionTextView)
        optionButtons = listOf(
            findViewById(R.id.optionAButton),
            findViewById(R.id.optionBButton),
            findViewById(R.id.optionCButton),
            findViewById(R.id.optionDButton)
        )
        nextButton = findViewById(R.id.nextButton)

        // Get quiz data passed from the previous activity as a JSON string
        val quizJson = intent.getStringExtra("QUIZ_JSON") ?: ""
        val explanation = intent.getStringExtra("EXPLANATION")

        // Log the received JSON data for debugging purposes
        Log.d("QuizDebug", "Received Quiz JSON: $quizJson")

        // If JSON data is not empty, parse and load it into the quizData array
        if (quizJson.isNotEmpty()) {
            try {
                // Clean the JSON string by removing unwanted parts and load it into the quizData array
                val cleanedJson = quizJson.replace("```json", "").replace("```", "").trim()
                quizData = JSONArray(cleanedJson)
                totalQuestions = quizData.length()
                progressBar.max = totalQuestions // Set the maximum value of the progress bar
                showNextQuestion() // Display the first question
            } catch (e: JSONException) {
                // Handle JSON parsing errors and show a message to the user
                Log.e("QuizDebug", "Error parsing quiz JSON", e)
                Toast.makeText(this, "Error loading quiz", Toast.LENGTH_SHORT).show()
                finish() // Close the activity
            }
        } else {
            // If no quiz data is received, show an error message and close the activity
            Toast.makeText(this, "No quiz data received", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up a click listener for the "Next" button to show the next question
        nextButton.setOnClickListener {
            showNextQuestion()
        }
    }

    // Function to update the progress bar and text based on the current question
    private fun updateProgress() {
        val currentQuestion = currentQuestionIndex + 1
        progressTextView.text = "Question $currentQuestion/$totalQuestions"

        // Animate the progress bar's movement
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, currentQuestion)
            .setDuration(300) // Animation duration in milliseconds
            .start()

        // Animate the progress text to make it more dynamic
        val animator = ValueAnimator.ofInt(progressBar.progress, currentQuestion)
        animator.duration = 300 // Animation duration in milliseconds
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            progressTextView.text = "Question $animatedValue/$totalQuestions"
        }
        animator.start()
    }

    // Function to display the next quiz question
    private fun showNextQuestion() {
        currentQuestionIndex++ // Move to the next question
        updateProgress() // Update the progress bar and text
        resetButtonColors() // Reset the colors of the option buttons

        // If all questions are answered, show the quiz result
        if (currentQuestionIndex >= quizData.length()) {
            showQuizResult()
            return
        }

        // Try to load and display the next question
        try {
            val questionObject = quizData.getJSONObject(currentQuestionIndex)
            val question = questionObject.getString("question")
            val options = questionObject.getJSONArray("options")
            val correctAnswer = questionObject.getInt("correctAnswer")

            // Set the question text and options for the current question
            questionTextView.text = question
            for (i in optionButtons.indices) {
                optionButtons[i].text = options.getString(i)
                optionButtons[i].isEnabled = true // Enable the buttons for answering
                optionButtons[i].setOnClickListener {
                    checkAnswer(i, correctAnswer) // Check if the selected answer is correct
                }
            }

            nextButton.visibility = View.GONE // Hide the "Next" button until the user answers
        } catch (e: JSONException) {
            // Handle any errors while displaying the question
            Log.e("QuizDebug", "Error displaying question", e)
            Toast.makeText(this, "Error displaying question", Toast.LENGTH_SHORT).show()
            showNextQuestion() // Skip to the next question if there is an error
        }
    }

    // Function to check if the selected answer is correct
    private fun checkAnswer(selectedIndex: Int, correctAnswer: Int) {
        // Get colors from resources for correct and incorrect answers
        val correctColor = ContextCompat.getColorStateList(this, R.color.green)
        val incorrectColor = ContextCompat.getColorStateList(this, R.color.red)

        // Check if the selected answer is correct
        if (selectedIndex == correctAnswer) {
            score++ // Increase the score for a correct answer
            optionButtons[selectedIndex].backgroundTintList = correctColor
//            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            // If incorrect, highlight the correct answer and show a message
            optionButtons[selectedIndex].backgroundTintList = incorrectColor
            optionButtons[correctAnswer].backgroundTintList = correctColor
//            Toast.makeText(
//                this,
//                "Incorrect. The correct answer was ${optionButtons[correctAnswer].text}",
//                Toast.LENGTH_SHORT
//            ).show()
        }

        // Disable all buttons after answering and show the "Next" button
        optionButtons.forEach { it.isEnabled = false }
        nextButton.visibility = View.VISIBLE
    }

    // Function to reset the button colors for the next question
    private fun resetButtonColors() {
        val defaultColor = ContextCompat.getColorStateList(this, R.color.primary_very_light)
        optionButtons.forEach {
            it.backgroundTintList = defaultColor
        }
    }

    // Function to show the quiz result after all questions are answered
    private fun showQuizResult() {
        questionTextView.text = "Quiz complete!" // Display a completion message
        val resultView= findViewById<TextView>(R.id.tvresult)
        optionButtons.forEach { it.visibility = View.GONE } // Hide the option buttons
        nextButton.visibility = View.GONE // Hide the "Next" button
        progressBar.visibility = View.GONE // Hide the progress bar
        progressTextView.visibility = View.GONE // Hide the progress text

        // Display the user's score
        val resultText = "Your score: $score out of $totalQuestions"


//        Toast.makeText(this, resultText, Toast.LENGTH_LONG).show()
        resultView.visibility = View.VISIBLE

        resultView.text = resultText
        //show result



        // Determine which video to play based on the score
        val videoResource = when {
            score <= SCORE_LOW -> R.raw.padai // Low score
            score == SCORE_HIGH -> R.raw.adbhut // High score
            else -> R.raw.itnagalat // Medium score
        }

        // Play the appropriate video
        playVideo(videoResource)
    }

    // Function to play a video based on the resource ID
    private fun playVideo(videoResource: Int) {
        // Find the VideoView in the layout
        val videoView: VideoView = findViewById(R.id.videoView)

        videoView.visibility = View.VISIBLE // Make the VideoView visible

        // Create the path to the video resource
        val videoPath = "android.resource://$packageName/$videoResource"
        videoView.setVideoURI(Uri.parse(videoPath)) // Set the video URI
        videoView.setMediaController(MediaController(this)) // Add media controls for playback
        videoView.requestFocus() // Focus on the VideoView for playback

        // Set a listener to restart the video after it completes
        videoView.setOnCompletionListener { mp ->
            mp.start() // Restart the video
        }

        videoView.start() // Start the video playback
    }
}
