package it.francesco.provacondivisione

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

/**
 * App di prova: serve SOLO a capire se WhatsApp tiene il testo
 * come didascalia quando condividiamo foto + testo verso lo stato.
 */
class MainActivity : ComponentActivity() {

    // Copia locale della foto scelta, pronta da condividere
    private var fotoDaCondividere: Uri? = null

    private lateinit var anteprima: ImageView
    private lateinit var testo: EditText
    private lateinit var stato: TextView

    // Selettore foto di sistema: non serve nessun permesso
    private val scegliFoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) preparaFoto(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        anteprima = findViewById(R.id.anteprima)
        testo = findViewById(R.id.testo)
        stato = findViewById(R.id.stato)

        findViewById<Button>(R.id.btnScegli).setOnClickListener {
            scegliFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<Button>(R.id.btnMenu).setOnClickListener { condividi(pacchetto = null) }
        findViewById<Button>(R.id.btnBusiness).setOnClickListener { condividi("com.whatsapp.w4b") }
        findViewById<Button>(R.id.btnWhatsapp).setOnClickListener { condividi("com.whatsapp") }
        findViewById<Button>(R.id.btnCopia).setOnClickListener { copiaTesto() }
    }

    /** Copia la foto scelta nella cache dell'app, così possiamo passarla a WhatsApp. */
    private fun preparaFoto(origine: Uri) {
        val cartella = File(cacheDir, "condivisi").apply { mkdirs() }
        val file = File(cartella, "foto.jpg")
        contentResolver.openInputStream(origine)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        fotoDaCondividere = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

        // Ricarica l'anteprima (azzerando prima, sennò resta la vecchia immagine)
        anteprima.setImageURI(null)
        anteprima.setImageURI(fotoDaCondividere)
        stato.text = "Foto pronta ✔"
    }

    /**
     * Condivide foto + testo.
     * pacchetto = null  -> mostra il menu "Condividi" di Android
     * pacchetto = "..." -> apre direttamente quell'app
     */
    private fun condividi(pacchetto: String?) {
        val foto = fotoDaCondividere
        if (foto == null) {
            Toast.makeText(this, "Prima scegli una foto", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, foto)                  // la foto
            putExtra(Intent.EXTRA_TEXT, testo.text.toString())   // il testo (didascalia)
            clipData = ClipData.newRawUri("foto", foto)          // necessario per dare il permesso di lettura
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (pacchetto == null) {
            startActivity(Intent.createChooser(intent, "Condividi con…"))
            return
        }

        intent.setPackage(pacchetto)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "$pacchetto non è installato", Toast.LENGTH_LONG).show()
        }
    }

    /** Piano B: se la didascalia non passa, il testo si incolla a mano. */
    private fun copiaTesto() {
        val appunti = getSystemService(ClipboardManager::class.java)
        appunti.setPrimaryClip(ClipData.newPlainText("didascalia", testo.text.toString()))
        Toast.makeText(this, "Testo copiato", Toast.LENGTH_SHORT).show()
    }
}
