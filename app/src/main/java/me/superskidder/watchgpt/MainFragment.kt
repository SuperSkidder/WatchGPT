import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.gson.GsonBuilder
import me.superskidder.watchgpt.R
import me.superskidder.watchgpt.config.SimpleConfig
import me.superskidder.watchgpt.data.ChatCompletionMessage
import me.superskidder.watchgpt.data.ChatGPTApi
import me.superskidder.watchgpt.data.ChatGPTRequest
import me.superskidder.watchgpt.data.ChatGPTResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainFragment : Fragment() {
    private lateinit var messageText: EditText
    private lateinit var messageView: WebView
    private lateinit var messageScroll: ScrollView
    private lateinit var sendButton: Button
    private lateinit var newConversationButton: Button
    private lateinit var hint: TextView

    private var apiKey = ""
    var systemPrompt = "You are a helpful assistant."

    private var messages: List<ChatCompletionMessage> =
        listOf(ChatCompletionMessage("system", systemPrompt))

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
        messageView.settings.loadsImagesAutomatically = true

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
        newConversationButton.setOnClickListener {
            messageView.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null)

            messages = listOf(ChatCompletionMessage("system", systemPrompt))
            sendButton.isClickable = true
        }
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
                messageView.evaluateJavascript(
                    "window.scrollTo(0, document.body.scrollHeight);",
                    null
                )
                sendButton.isClickable = true
            } else {
                sendButton.isClickable = true
                msgBox("错误", "请求超时", "了解")
            }
        }
    }

    private fun update() {
        val markdown = buildString {
            messages.forEach { message ->
                appendLine(
                    "${
                        when (message.role) {
                            "user" -> "**你**"
                            "assistant" -> "**GPT**"
                            else -> {
                                "系统"
                            }
                        }
                    }: ${message.content}\n"
                )
            }
        }

        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()
        val document: Node = parser.parse(markdown)

        // 将节点树转换为HTML文本
        var html = renderer.render(document)
        // 代码高亮
        html = """
        <html>
        <head>
        <style>
        body { color:white }
        </style>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.3.1/styles/default.min.css">
        <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.3.1/highlight.min.js"></script>
        <script>hljs.highlightAll();</script>
        </head>
        <body>
        $html
        </body>
        </html>
    """.trimIndent()
        // 在WebView中显示HTML文本
        messageView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
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
