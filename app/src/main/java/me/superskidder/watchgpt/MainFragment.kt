package me.superskidder.watchgpt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.gson.GsonBuilder
import me.superskidder.watchgpt.config.SimpleConfig
import me.superskidder.watchgpt.data.ChatCompletionMessage
import me.superskidder.watchgpt.data.ChatGPTApi
import me.superskidder.watchgpt.data.ChatGPTRequest
import me.superskidder.watchgpt.data.ChatGPTResponse
import me.superskidder.watchgpt.history.messages
import me.superskidder.watchgpt.history.readHistory
import me.superskidder.watchgpt.history.saveHistory
import me.superskidder.watchgpt.speaking.BaiduTranslator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class MainFragment : Fragment() {
    private lateinit var messageText: EditText
    private lateinit var messageView: TextView
    private lateinit var messageScroll: ScrollView
    private lateinit var sendButton: Button
    private lateinit var newConversationButton: Button
    private lateinit var hint: TextView
    private lateinit var baiduTranslator: BaiduTranslator

    private var apiKey = ""
    var systemPrompt = "You are a helpful assistant."



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize views
        hint = view.findViewById(R.id.hint)

        messageText = view.findViewById(R.id.messageText)
        messageScroll = view.findViewById(R.id.messageScroll)
        sendButton = view.findViewById(R.id.sendButton)
        newConversationButton = view.findViewById(R.id.newConversationButton)

        messageView = view.findViewById(R.id.messageView)
        messageView.setBackgroundColor(resources.getColor(android.R.color.black))

        sendButton.setOnClickListener {
            sendMessage(messageText.text.toString())
        }
        hint.setOnClickListener {
            msgBox(
                "WatchGPT", "由SuperSkidder用❤开发\n" +
                        "QQ:2869049757\n" +
                        "API中转提供：©Cloudflare\n", "明白"
            )
        }
        val configManager = SimpleConfig(requireContext())

        systemPrompt = configManager.getString("systemPrompt", "").toString()
        messages = listOf(ChatCompletionMessage("system", systemPrompt))
        newConversationButton.setOnClickListener {
            messageView.text = ""
            messages = listOf(ChatCompletionMessage("system", systemPrompt))
            sendButton.isClickable = true
        }

        if (configManager.getBoolean("isFirstStartup", true)) {
            msgBox(
                "第一次使用？",
                "左滑可以切换页面，第一次使用需配置apikey 获取apikey: platform.openai.com(需要有chatgpt账号)\n" +
                        "修改配置项后必须点击保存才能正常使用哦~\n" +
                        "systemprompt是给AI的情景预设，你可以指定它的职责或特定回答方式，如：充当一名英语教师、充当Linux终端、扮演我的朋友 等\n" +
                        "点击聊天界面的笔图标可以清除记录~\n" +
                        "加入WatchGPT QQ交流群：756647981",
                "完成"
            )
            configManager.setBoolean("isFirstStartup", false)
        }

        baiduTranslator = BaiduTranslator()
        readHistory(requireContext())
        update()
    }

    fun msgBox(title: String, content: String, btn: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(content)
        builder.setPositiveButton(btn) { _, _ ->
            // 点击确定按钮的代码
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveHistory(requireContext())
    }


    private fun sendMessage(message: String) {
        val configManager = SimpleConfig(requireContext())
        apiKey = configManager.getString("apikey", "").toString()
        systemPrompt = configManager.getString("systemPrompt", "").toString()
        val gptTypeValue = configManager.getString("gptType", "gpt-3.5-turbo")

        val request = ChatGPTRequest(
            gptTypeValue, messages + listOf(
                ChatCompletionMessage("user", message),
            )
        )
        messages = messages + ChatCompletionMessage(
            "user",
            message
        )
        update()
        messageText.setText("")
        sendButton.isClickable = false
        requestCompletion(request) { response ->
            if (response != null) {
                val choice = response.choices[0]
                val generatedText = choice.message.content
                messages = messages + ChatCompletionMessage("assistant", generatedText)
                messageScroll.post { messageScroll.fullScroll(View.FOCUS_DOWN) }
                update()
                //滑动到页面最下方
                messageScroll.post { messageScroll.fullScroll(ScrollView.FOCUS_DOWN) }
                if (configManager.getBoolean("speakout",false)){
                    baiduTranslator.speak(generatedText,"zh")
                }
                sendButton.isClickable = true
            } else {
                sendButton.isClickable = true
                msgBox("错误", "请求超时", "了解")
            }
        }
    }

    private fun update() {
        val markdown = buildString {
            val usemessages = messages.drop(1)
            usemessages.forEach { message ->
                appendLine(
                    "${
                        when (message.role) {
                            "user" -> "你"
                            "assistant" -> "GPT"
                            else -> {
                                "Unknown"
                            }
                        }
                    }: ${message.content}\n"
                )
            }
        }

        messageView.text = markdown
    }

    private fun requestCompletion(request: ChatGPTRequest, callback: (ChatGPTResponse?) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 无限制
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://chat.skidder.life/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

        val chatGPTApi = retrofit.create(ChatGPTApi::class.java)
        val call = chatGPTApi.getCompletion("Bearer $apiKey", request)

        call.enqueue(object : Callback<ChatGPTResponse> {
            override fun onResponse(
                call: Call<ChatGPTResponse>,
                response: Response<ChatGPTResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        callback(it)
                    }
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<ChatGPTResponse>, t: Throwable) {
                callback(null)
            }
        })
    }

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}
