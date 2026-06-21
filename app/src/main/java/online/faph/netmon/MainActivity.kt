package online.faph.netmon

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 72, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "NetMon Agent"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Sukatin ang totoong internet usage mo para sa WiFiCo. Walang personal data na kinukuha — bytes lang."
            textSize = 14f
            setPadding(0, 10, 0, 28)
        })

        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 28, 0, 0)
        }

        if (Prefs.isRegistered(this)) renderActive(root) else renderRegister(root)
        root.addView(status)

        setContentView(
            ScrollView(this).apply { addView(root) },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun renderRegister(root: LinearLayout) {
        val nick = EditText(this).apply {
            hint = "Nickname mo"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val btn = Button(this).apply { text = "Sumali sa NetMon" }
        btn.setOnClickListener {
            val n = nick.text.toString().trim()
            if (n.length < 2) { toast("Masyadong maikli ang nickname"); return@setOnClickListener }
            btn.isEnabled = false
            status.text = "Nagrerehistro..."
            thread {
                val uid = Api.register(this, n)
                runOnUiThread {
                    if (uid != null) {
                        PushWorker.schedule(this)
                        thread { Api.push(this) } // first sample -> establish byte baseline
                        recreate()
                    } else {
                        btn.isEnabled = true
                        status.text = "Hindi nag-register — baka kuha na ang nickname o walang internet. Subukan ulit."
                    }
                }
            }
        }
        root.addView(nick)
        root.addView(btn)
    }

    private fun renderActive(root: LinearLayout) {
        val nick = Prefs.getNickname(this) ?: "?"
        root.addView(TextView(this).apply {
            text = "\u2705 Aktibo bilang @$nick\nAuto-push kada ~15 min kapag may internet. Iwan lang naka-install."
            textSize = 15f
        })
        val pushBtn = Button(this).apply { text = "Push ngayon (test)" }
        pushBtn.setOnClickListener {
            pushBtn.isEnabled = false
            status.text = "Nagpu-push..."
            thread {
                val ok = Api.push(this)
                runOnUiThread {
                    pushBtn.isEnabled = true
                    status.text = if (ok) "\u2705 Pushed!" else "\u274C Push failed \u2014 i-check ang internet."
                }
            }
        }
        root.addView(pushBtn)
        PushWorker.schedule(this) // ensure the periodic job is scheduled
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
