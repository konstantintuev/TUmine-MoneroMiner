package tuev.co.monerominer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import tuev.co.tumine.*
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.full.memberProperties

class MainActivity : AppCompatActivity() {

    private lateinit var infoPassing: InfoPassing
    private lateinit var preferences: SharedPreferences
    private var mineConnector: MineConnector? = null

    private val outputInfoTextViews = HashMap<String, TextView>()

    private var mOutputInfo = OutputInfo()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)


        infoPassing = InfoPassing(this@MainActivity)

        infoPassing.smartStart = true

        val cores = infoPassing.availableThreads
        // write suggested cores usage into editText
        var suggested = if (preferences.contains("threads")) preferences.getInt("threads", 0) else cores / 2
        if (preferences.contains("username")) {
            username.setText(preferences.getString("username", ""))
        }
        if (preferences.contains("password")) {
            password.setText(preferences.getString("password", ""))
        }
        if (suggested <= 1) suggested = 1
        threads.setText(Integer.toString(suggested))
        allthreads.setText(Integer.toString(cores))
        username.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                preferences.edit().putString("username", p0?.toString() ?: "").apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                preferences.edit().putString("password", p0?.toString() ?: "").apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        threads.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if(p0?.isEmpty() == false) {
                    preferences.edit().putInt("threads", p0.toString().toInt()).apply()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
        start.setOnClickListener { startMining() }
        stop.setOnClickListener { stopMining() }
        check.setOnClickListener {
            val url = "https://android-miner.tuev-co.eu"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        advhash.setOnClickListener {
            mineConnector?.requestHashratePerThread()
        }
        AlertDialog.Builder(this)
                .setTitle("Performance Warning!")
                .setMessage("All the colored information presented is only for aesthetic and demonstration purposes.\n" +
                        "It uses Spannable String and Reflection and is EXTREMELY inefficiently generated!")
                .setPositiveButton("OK") { _, _ -> }
                .show()
        updateOutputInfo(true)

    }

    private fun updateOutputInfo(create: Boolean) {
        for (prop in OutputInfo::class.memberProperties) {
            /*val layoutInflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row: LinearLayout = layoutInflater.inflate(R.layout.output_row, null) as LinearLayout
            row.findViewById<TextView>(R.id.title).text = prop.name*/
            if (create) {
                val itemText = TextView(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    itemText.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
                } else {
                    itemText.setTextAppearance(this, R.style.TextAppearance_AppCompat_Caption)
                }
                itemText.setText(reflectionToString(prop.name, prop.get(mOutputInfo)),  TextView.BufferType.SPANNABLE)
                outputInfoTextViews[prop.name] = itemText
                output_info.addView(itemText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            } else {
                val text = outputInfoTextViews[prop.name]
                text?.setText(reflectionToString(prop.name, prop.get(mOutputInfo)),  TextView.BufferType.SPANNABLE)
            }
        }
    }

    private val green: Int = Color.parseColor("#4CAF50")
    private val blue: Int = Color.parseColor("#03A9F4")
    private val blgray: Int = Color.parseColor("#CFD8DC")
    private val red: Int = Color.parseColor("#f44336")

    fun reflectionToString(propName: String, obj: Any?): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (obj == null) {
            val start = builder.length
            builder.append("No data for $propName\n")
            builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(Color.LTGRAY), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return builder
        }
        var start = builder.length
        builder.append("${propName.capitalize()}:\n")
        builder.setSpan(ForegroundColorSpan(green), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(1.4f), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (basicObjectToStirng(builder, obj, blgray)) {
            return builder
        }

        for (prop in obj.javaClass.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }) {
            prop.isAccessible = true
            val value = prop.get(obj)
            if (!value?.toString().isNullOrBlank()) {
                start = builder.length
                builder.append("${prop.name.capitalize()}: ")
                val basic = value is String || value is Boolean || value is Int || value is Float || value is Enum<*>
                if (basic) {
                    builder.setSpan(ForegroundColorSpan(blue), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    builder.setSpan(ForegroundColorSpan(green), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                if (value is List<*>) {
                    start = builder.length
                    builder.append("${value.joinToString(" | ")}\n\n")
                    builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (!basicObjectToStirng(builder, value, blgray)) {
                    val clazz: Class<*>? = value.javaClass
                    val fields = clazz?.declaredFields
                    if (fields != null) {
                        for (f in fields) {
                            if (!Modifier.isStatic(f.modifiers)) {
                                try {
                                    f.isAccessible = true
                                    start = builder.length
                                    builder.append("${f.name.capitalize()}: ")
                                    builder.setSpan(ForegroundColorSpan(blue), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                    start = builder.length
                                    builder.append("${f.get(value)}\n")
                                    builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                } catch (e: IllegalAccessException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    builder.append("\n")
                }
            }
        }
        //builder.append("\n")
        return builder
    }

    fun basicObjectToStirng(builder: SpannableStringBuilder, obj: Any, spanColor: Int): Boolean {
        var start: Int
        if (obj is String || obj is Boolean || obj is Int || obj is Float ) {
            start = builder.length
            builder.append("$obj\n")
            builder.setSpan(ForegroundColorSpan(spanColor), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }
        if (obj is Enum<*>) {
            start = builder.length
            builder.append("${obj.name}\n")
            builder.setSpan(ForegroundColorSpan(spanColor), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }
        if (obj is ArrayList<*>) {
            for (item in obj) {
                val clazz: Class<*>? = item.javaClass
                val fields = clazz?.declaredFields
                if (fields != null) {
                    for (f in fields) {
                        if (!Modifier.isStatic(f.modifiers)) {
                            try {
                                f.isAccessible = true
                                start = builder.length
                                builder.append("${f.name.capitalize()}: ")
                                builder.setSpan(ForegroundColorSpan(blue), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                                start = builder.length
                                builder.append("${f.get(item)}\n")
                                builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            } catch (e: IllegalAccessException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                builder.append("\n")
            }
            return true
        }
        return false
    }

    private var running: Boolean = false

    private fun startMining() {
        if (running) {
            Toast.makeText(this, "It's already running, WHY YOU DO THIS?", Toast.LENGTH_LONG).show()
            return
        }
        running = true
        if (mineConnector == null) {
            mineConnector = MineConnector(object : OnMessageReceived {
                override fun connected() {
                    Toast.makeText(this@MainActivity, "Connected to Miner", Toast.LENGTH_LONG).show()
                }

                override fun messageReceived(message: OutputInfo) {
                    mOutputInfo = message
                    Log.wtf("recv", "messageReceived")
                    when (message.lastChangedValue) {
                        OutputHelperClasses.ChangedValue.hashrate -> {
                            speed.text = message.hashrate?.Highest?.toString() ?: "0"
                        }
                        OutputHelperClasses.ChangedValue.lastMiningJobResult -> {
                            accepted.text = message.lastMiningJobResult?.accepted?.toString() ?: "0"
                        }
                    }

                    for (prop in OutputInfo::class.memberProperties) {
                        if (prop.name == message.lastChangedValue?.name) {
                            val view = outputInfoTextViews[prop.name] ?: break
                            if (message.lastChangedValue == OutputHelperClasses.ChangedValue.lastMiningJobResult
                                    || message.lastChangedValue == OutputHelperClasses.ChangedValue.lastMiningJob
                                    || message.lastChangedValue == OutputHelperClasses.ChangedValue.lastError) {
                                scroll_view.post {
                                    scroll_view.smoothScrollTo(0, view.bottom)
                                }
                            }
                            view.setText(reflectionToString(prop.name, prop.get(mOutputInfo)), TextView.BufferType.SPANNABLE)
                            val builder = SpannableStringBuilder()
                            var start = builder.length
                            builder.append("LastChangedValue:\n")
                            builder.setSpan(ForegroundColorSpan(green), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            builder.setSpan(RelativeSizeSpan(1.4f), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            start = builder.length
                            builder.append("${prop.name}\n")
                            builder.setSpan(ForegroundColorSpan(blgray), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            outputInfoTextViews["lastChangedValue"]?.setText(builder, TextView.BufferType.SPANNABLE)
                            break
                        }
                    }
                }
            }, this, infoPassing)
        }
        infoPassing.pools = ArrayList()
        infoPassing.pools.add(Pool(pool.text.toString(), username.text.toString(), password.text.toString()))
        infoPassing.threadsToUse = Integer.parseInt(threads.text.toString())
        infoPassing.updateOverInternet = false
        infoPassing.debugParams = true
        //infoPassing.cpuPriority = 5
        val channelId2 = "miner_info"
        val mNotifyManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelRetr2: NotificationChannel?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannelRetr2 = mNotifyManager.getNotificationChannel(channelId2)
            if (notificationChannelRetr2 == null) {
                val notificationChannel: NotificationChannel
                val channelName = "Info"
                val importance = NotificationManager.IMPORTANCE_MIN
                notificationChannel = NotificationChannel(channelId2, channelName, importance)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                mNotifyManager.createNotificationChannel(notificationChannel)
            }
        }
        val notification = NotificationCompat.Builder(this@MainActivity, channelId2)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Info")
                .setContentText("This mines XMR for the hardworking devs").build()
        infoPassing.notification = notification
        infoPassing.isBasicLogging = true

        Log.d(javaClass.simpleName, "startMineService")
        infoPassing.startMiningService()
    }

    private fun stopMining() {
        infoPassing.stopMiningService()
        running = false
        accepted.text = "0"
        speed.text = "0"
        mOutputInfo = OutputInfo()
        updateOutputInfo(false)
    }

    //recommended but if not done and the activity is running, the service will be less likely to be killed
    override fun onDestroy() {
        mineConnector?.detach()
        super.onDestroy()
    }
}
