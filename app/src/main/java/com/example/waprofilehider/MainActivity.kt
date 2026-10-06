package com.example.waprofilehider

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 40, 32, 32) }
        val title = TextView(this).apply { text = "הסתרת תמונות WhatsApp"; textSize = 24f; gravity = Gravity.CENTER; setPadding(0,0,0,24) }
        val info = TextView(this).apply {
            text = "האפליקציה מיועדת להסתיר תמיד תמונות פרופיל ב-WhatsApp באמצעות שירות נגישות.\n\n1. הפעל את שירות הנגישות פעם אחת.\n2. לאחר מכן פתח את WhatsApp כרגיל.\n3. תמונות פרופיל מזוהות יכוסו אוטומטית בעיגול ניטרלי.\n\nאין מתג הפעלה בתוך האפליקציה — כאשר שירות הנגישות פעיל, ההסתרה פעילה תמיד."
            textSize = 16f; setPadding(0,0,0,24)
        }
        val open = Button(this).apply { text = "פתח הגדרות נגישות"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        val status = TextView(this).apply { textSize = 15f; setPadding(0,24,0,0) }
        root.addView(title); root.addView(info); root.addView(open); root.addView(status)
        setContentView(root)
        status.text = "שירות נגישות: " + if (AvatarHiderService.isEnabled) "פעיל" else "לא פעיל"
    }
    override fun onResume() { super.onResume(); (findViewById<LinearLayout>(android.R.id.content)?.getChildAt(0) as? LinearLayout)?.let { } }
}
