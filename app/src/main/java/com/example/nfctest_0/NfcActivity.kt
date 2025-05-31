package com.example.nfctest_0

import android.app.PendingIntent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.PendingIntent.FLAG_MUTABLE
import android.widget.TextView
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.NdefMessage
import android.util.Log
import android.widget.Switch
import android.nfc.NdefRecord



class NfcActivity : AppCompatActivity() {
    private var mNfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nfc)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_MUTABLE)

// 受け取るIntentを指定
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))

// 反応するタグの種類を指定
        techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(android.nfc.tech.NdefFormatable::class.java.name))

        mNfcAdapter = NfcAdapter.getDefaultAdapter(applicationContext)
    }
    override fun onResume() {
        super.onResume()

// NFCタグの検出を有効化
        mNfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)
    }

    /**
     * NFCタグの検出時に呼ばれる
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        var switchMode :Switch = findViewById<Switch>(R.id.switchMode)
        if(switchMode.isChecked() == false) {
// タグのIDを取得
            val tagId: ByteArray = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return

            var list = ArrayList<String>()
            for (byte in tagId) {
                list.add(String.format("%02X", byte.toInt() and 0xFF))
            }

// 画面に表示
            var tagTextView: TextView = findViewById(R.id.textViewIDM)
            tagTextView.text = list.joinToString(":")
//データ読み込み
            if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
            ) {

                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                /*val ndef = */Ndef.get(tag) ?: return
                val raws = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return
                var msgs = arrayOfNulls<NdefMessage>(raws.size)
                var textbuffer = ""

                for (i in raws.indices) {
                    msgs[i] = raws[i] as NdefMessage?
                    if (msgs[i] != null) {
                        for (record in msgs[i]?.records!!) {

                            Log.d("TAG", "TNF：" + record.tnf)
                            Log.d("TAG", "Type：" + String(record.type))

// payload（データ本体）
                            Log.d("TAG", "payload：" + String(record.payload))
// payloadからメッセージ部分を抽出
                            Log.d(
                                "TAG",
                                "payload-message：" + String(
                                    record.payload,
                                    3,
                                    record.payload.size - 3
                                )
                            )

// payloadの中身を1byteずつ表示
                            for (i in record.payload.indices) {
                                Log.d(
                                    "TAG", String.format(
                                        "payload[%d] : 0x%02x / %c",
                                        i,
                                        record.payload[i].toInt() and 0xFF,
                                        record.payload[i].toInt() and 0xFF
                                    )
                                )
                            }
                            textbuffer += String(record.payload, 3, record.payload.size - 3)
                            textbuffer += "\r"

                            var textViewReceiveDataStr: TextView =
                                findViewById(R.id.textViewReceiveData)
                            textViewReceiveDataStr.text = textbuffer
                        }
                    }
                }
            }
        }else{
            var editTextSendData :TextView = findViewById<TextView>(R.id.editTextSendData)
            val text = editTextSendData.text.toString()

            if(NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                val ndef = Ndef.get(tag) ?: return

                if(ndef.isWritable) {

                    val record = NdefRecord.createTextRecord("en", text)
                    val msg = NdefMessage(record);

                    var textViewSendDataStr: TextView =
                        findViewById(R.id.textViewSendData)
                    textViewSendDataStr.text = text

                    ndef.connect()
                    ndef.writeNdefMessage(msg)
                    ndef.close()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        mNfcAdapter?.disableForegroundDispatch(this)
    }
}