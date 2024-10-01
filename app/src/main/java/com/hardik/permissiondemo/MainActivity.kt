package com.hardik.permissiondemo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.hardik.permissiondemo.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

/**
 * Uou should request for permission with request code.
 *
 * @author hardik
 * @since 2024/08/12
 */
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private lateinit var READ_EXTERNAL_STORAGE :String
        private lateinit var WRITE_EXTERNAL_STORAGE :String

        private const val REQUEST_CODE_OPEN_DOCUMENT = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, now open document
                openDocumentLauncher.launch(arrayOf("*/*")) // Corrected to use array of MIME types
            } else {
                // Permission denied
                Log.e("Permission", "Read External Storage permission denied.")
            }
        }

        // Initialize document picker launcher
        openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                readFile(uri)
            }
        }

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

    }


    // ActivityResultLauncher for requesting permission
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // ActivityResultLauncher for picking a document
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
//    private lateinit var openDocumentLauncher: ActivityResultLauncher<Intent>



    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_MEDIA_AUDIO
            WRITE_EXTERNAL_STORAGE = android.Manifest.permission.READ_MEDIA_IMAGES
        }else {
            READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE
            WRITE_EXTERNAL_STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            else
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        binding.buttonPicFileOldApproach.setOnClickListener {
            pickFileForReadLegacyApproach()
        }
        binding.buttonPicFileModernApproach.setOnClickListener{
            pickFileForReadModernApproach()
        }

        binding.fab.setOnClickListener { view ->
            Handler(Looper.myLooper()!!).postDelayed({

                if (checkPermission()){
                    Log.d(TAG, "onResume: grant permission")
                    Toast.makeText(this, "Permission Already Granted", Toast.LENGTH_SHORT).show()

                    ifPermissionGranted() // call common function

                }else{
                    Log.d(TAG, "onResume: Requesting for permission grant!!")
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),PERMISSION_REQUEST_CODE)

                }
            }, 100)
        }
    }


    /**
     * First you should check permission is granted or not.
     *
     * @author hardik
     * @since 2024/08/12
     */
    private fun checkPermission():Boolean{
        val resultRead = ActivityCompat.checkSelfPermission(this@MainActivity, READ_EXTERNAL_STORAGE)
        val resultWrite = ActivityCompat.checkSelfPermission(this@MainActivity, WRITE_EXTERNAL_STORAGE)

        return resultRead == PackageManager.PERMISSION_GRANTED && resultWrite == PackageManager.PERMISSION_GRANTED
    }


    /**
     * Once you requesting for permissions, you got result here.
     *
     * @author hardik
     * @since 2024/08/12
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE){
            Log.d(TAG, "onRequestPermissionsResult: requestCode: $requestCode")
            if (grantResults.isNotEmpty()){
                Log.d(TAG, "onRequestPermissionsResult: permissions: is not empty")

                val resultRead = grantResults[0]
                val resultWrite = grantResults[1]

                val checkRead = resultRead == PackageManager.PERMISSION_GRANTED
                val checkWrite = resultWrite == PackageManager.PERMISSION_GRANTED

                if (checkRead && checkWrite){
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted")
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    ifPermissionGranted() // call common function
                }else{
                    Log.d(TAG, "onRequestPermissionsResult: Permission denied")
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    // TODO: show message to user that permission denied and ask again

                }
            }
        }

    }


    /**
     * User allow it's to create file.
     *
     * @author hardik
     * @since 2024/08/12
     */
    private fun ifPermissionGranted(){
        Log.d(TAG, "ifPermissionGranted: ")
        // TODO: do your work here after permission granted

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            saveFileToMediaStore(this,"hardik.txt","Hi Hardik!")
        }else{
            createFileInCustomDir("hardik1.txt","Hi Hardik!")
        }
    }


    /**
     * Android 9 (API 28, Version code Pie) and below, use app-specific directories on external storage and create file.
     *
     * @author hardik
     * @since 2024/08/12
     */
    private fun createFileInCustomDir(fileName: String, content: String) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            // Define your custom path
            val customDir = File(Environment.getExternalStorageDirectory(), "PermissionDemo/files")
            if (!customDir.exists()) {
                customDir.mkdirs() // Create directory if it doesn't exist
            }

            // Define the file path
            val file = File(customDir, fileName)
            Log.d(TAG, "createFileInCustomDir: filePath: ${file.absolutePath}")
            try {
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "createFileInCustomDir: IOException:",e.fillInStackTrace() )
            }
        } else {
            // Handle the case where external storage is not available
        }
    }

    /**
     * Android 10 (API 29, Version code Q) and above, use app-specific directories on external storage and create file.
     *
     * @author hardik
     * @since 2024/08/12
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveFileToMediaStore(context: Context, fileName: String, content: String) {
        // Define the directory within public storage
        val relativePath = "Documents/PermissionDemo/files" // or use another valid directory

        // Create a ContentValues object to define the file's metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        try {
            // Insert the file into MediaStore
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

            uri?.let {
                context.contentResolver.openOutputStream(it).use { outputStream ->
                    if (outputStream != null) {
                        outputStream.write(content.toByteArray())
                        outputStream.flush()
                        Log.d(TAG, "File saved successfully to MediaStore: $uri")
                    } else {
                        Log.e(TAG, "Failed to open output stream for URI: $uri")
                    }
                }

                // Retrieve file details using the URI
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val pathIndex = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                        val path = c.getString(pathIndex)
                        Log.d("FileLocation", "File path: $path")
                    } else {
                        Log.e(TAG, "Failed to retrieve file details from URI: $uri")
                    }
                }
            } ?: run {
                Log.e(TAG, "Failed to insert file into MediaStore.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "I/O error while saving file to MediaStore: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
        }
    }



    //------------------------------------------------------------------


    /**
     * Picking and reading files the old fashioned way
     *
     * @author hardik
     * @since 2024/08/13
     */
    private fun pickFileForReadLegacyApproach(){

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.setType("*/*")
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)// get result on onActivityResult()

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == RESULT_OK) {
            val uri = data?.data
            // Use the uri to access the file
            try {
                contentResolver.openInputStream(uri!!).use { inputStream ->
                    val reader =
                        BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        // Process the line
                        Log.d("FileContent", line!!)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }



    /**
     * New way to select and read files
     * using 'requestPermissionLauncher' or 'openDocumentLauncher' methods
     *
     * @author hardik
     * @since 2024/08/13
     */
    private fun pickFileForReadModernApproach(){
        // Request permission before picking a file
        requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
    }
    // Function to read the contents of the selected file
    private fun readFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        Log.d("FileContent", line ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileAccess", "Error reading file", e)
        }
    }



























    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }









    //----------------------------------------------------------------no user interaction
    fun callForRead(){
        val content = readFileInternal(this, "example.txt")
        val externalContent = readFileExternal(this, "example.txt")

        Log.e(TAG, "ifPermissionGranted: $content", )
        Log.e(TAG, "ifPermissionGranted: $externalContent", )
    }
    private fun readFileInternal(context: Context, fileName: String): String {
        return context.openFileInput(fileName).bufferedReader().use { it.readText() }
    }

    fun readFileExternal(context: Context, fileName: String): String {
        val file = File(context.getExternalFilesDir(null), fileName)
        return file.readText()
    }

}