package com.wandou.voicetoword;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.topeet.serialtest.senddata;

public class MainActivity extends Activity implements OnClickListener,
        OnSeekBarChangeListener {

    protected static final String TAG = "IatDemo";
    public Button mRecognize;
    public EditText mResultText;
    private SeekBar mSeekBar;
    private int seekBarProgress;
    private Toast mToast;
    public int times;
    private int ID;
    private LightCtrl mLight;
    public senddata data;
    private int progressnow;

    // �����������
    private TextUnderstander mTextUnderstander;
    // ������д����
    private SpeechRecognizer mIat;
    // ������дUI
    private RecognizerDialog mIatDialog;
    // ��HashMap�洢��д���
    private final HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    // ��������
    private final String mEngineType = SpeechConstant.TYPE_CLOUD;
    private SharedPreferences mSharedPreferences;

    int ret = 0; // �������÷���ֵ

    /**
     * ��дUI������
     */
    private final RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        /**
         * ʶ��ص�����.
         */
        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

    };

    /**
     * ��ʼ����������
     */
    private final InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("��ʼ��ʧ�ܣ������룺" + code);
            }
        }
    };

    /**
     * ��д��������
     */
    private final RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // �˻ص���ʾ��sdk�ڲ�¼�����Ѿ�׼�����ˣ��û����Կ�ʼ��������
            showTip("��ʼ˵��");
        }

        @Override
        public void onEndOfSpeech() {
            // �˻ص���ʾ����⵽��������β�˵㣬�Ѿ�����ʶ����̣����ٽ�����������
            showTip("����˵��");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips��
            // �����룺10118(��û��˵��)��������¼����Ȩ�ޱ�������Ҫ��ʾ�û���Ӧ�õ�¼��Ȩ�ޡ�
            // ���ʹ�ñ��ع��ܣ���ǣ���Ҫ��ʾ�û�������ǵ�¼��Ȩ�ޡ�
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // ���´������ڻ�ȡ���ƶ˵ĻỰid����ҵ�����ʱ���Ựid�ṩ������֧����Ա�������ڲ�ѯ�Ự��־����λ����ԭ��
            // ��ʹ�ñ����������ỰidΪnull
            // if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            // String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            // Log.d(TAG, "session id =" + sid);
            // }
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.e(TAG, results.getResultString());
            printResult(results);
            if (isLast) {
                // TODO ���Ľ��
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("��ǰ����˵����������С��" + volume);
            Log.d(TAG, "������Ƶ���ݣ�" + data.length);
        }
    };

    private final TextUnderstanderListener searchListener = new TextUnderstanderListener() {
        // �������ص�
        @Override
        public void onError(SpeechError error) {
            String text = "��������ʧ��";
            showTip(error.getPlainDescription(true));
            mResultText.setText(text);
            mResultText.setSelection(mResultText.length());
        }

        // �������ص�
        @Override
        public void onResult(UnderstanderResult result) {
            String text = result.getResultString();
            mResultText.setText(text);
            mResultText.setSelection(mResultText.length());
        }
    };

    private void changeSeekBarLevel() {
        // TODO Auto-generated method stub
        if (seekBarProgress >= 7) {
            seekBarProgress = 0;
        }
        mSeekBar.setProgress(seekBarProgress + 1);
    }

    private void closeLight() {
        // TODO Auto-generated method stub

    }

    private void initLayout() {
        // TODO Auto-generated method stub
        mRecognize = (Button) findViewById(R.id.button);
        mResultText = (EditText) findViewById(R.id.editText);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mRecognize.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    // ��ʼ��д
    // ����ж�һ����д������OnResult isLast=true ���� onError
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        mResultText.setText(null);// �����ʾ����
        mIatResults.clear();
        // ���ò���
        setParam();

        mTextUnderstander.setParameter(SpeechConstant.DOMAIN, "iat");
        times = 0;
        boolean isShowDialog = mSharedPreferences.getBoolean(
                getString(R.string.pref_key_iat_show), true);
        if (isShowDialog) {
            // ��ʾ��д�Ի���
            mIatDialog.setListener(mRecognizerDialogListener);
            mIatDialog.show();
            showTip(getString(R.string.text_begin));
        } else {
            // ����ʾ��д�Ի���
            ret = mIat.startListening(mRecognizerListener);
            if (ret != ErrorCode.SUCCESS) {
                showTip("��дʧ��,�����룺" + ret);
            } else {
                showTip(getString(R.string.text_begin));
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=55b71019");

        mLight = new LightCtrl();
        // ��ʼ��ʶ����UIʶ�����
        // ʹ��SpeechRecognizer���󣬿ɸ��ݻص���Ϣ�Զ�����棻
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this,
                mInitListener);
        mTextUnderstander = TextUnderstander.createTextUnderstander(
                MainActivity.this, mInitListener);
        // ��ʼ����дDialog�����ֻʹ����UI��д���ܣ����贴��SpeechRecognizer
        // ʹ��UI��д���ܣ������sdk�ļ�Ŀ¼�µ�notice.txt,���ò����ļ���ͼƬ��Դ
        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);

        mSharedPreferences = getSharedPreferences(this.getPackageName(),
                MODE_PRIVATE);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

    }

    // if ((resultText.trim()).equals("����")) {
    // changeSeekBarLevel();
    // }
    // if ((resultText.trim()).equals("�ر�") || (resultText.trim()).equals("�ص�"))
    // {
    // closeLight();
    // }

    // }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
        mLight.SetLight(progress);
        seekBarProgress = progress;
        new senddata().sent(mLight.GetLight());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // ��ȡjson����е�sn�ֶ�
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        String resultText = resultBuffer.toString();
        // mResultText.setText(resultText);
        // mResultText.setSelection(mResultText.length());

        text = resultText;
        if (times == 0) {
            ID = InstructionsAnalyze.InstructionsAnalyze(text);
            Toast.makeText(MainActivity.this, String.format("ID = %d", ID),
                    Toast.LENGTH_SHORT).show();
            Log.e("Reutrn ID", String.valueOf(ID));
            if (ID != 0) {
                // ID=Msb.getProgress();
                // sb.setProgress(0);
                // SBEditor.putInt("ID", ID);
                // new senddata().sent(ID);
                mLight.SetLight(mSeekBar.getProgress());
                switch (ID) {
                case 1:// turn on
                    if (mLight.GetLight() == 0) {
                        mLight.SetLight(4);
                        mSeekBar.setProgress(4);
                    } else {
                        mSeekBar.setProgress(mLight.GetLight());
                    }
                    break;
                case 2:// turn off
                    mLight.SetLight(0);
                    mSeekBar.setProgress(0);
                    break;
                case 3:// increase
                    mLight.Up(1);
                    mSeekBar.setProgress(mLight.GetLight());
                    break;
                case 4:// decrease
                    mLight.Down(1);
                    mSeekBar.setProgress(mLight.GetLight());
                    break;
                case 5:
                    new senddata().sent(9);
                default:
                }
            } else {

                // flytek AI resoponse here
                // Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT)
                // .show();
                String testtext = "ţ�ٵ�һ����";
                resultText = testtext;
                // ��ʼ��������
                mTextUnderstander.understandText(resultText, searchListener);
            }
            times++;
        }
    }

    /**
     * ��������
     * 
     * @param param
     * @return
     */
    public void setParam() {
        // ��ղ���
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // ������д����
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // ���÷��ؽ����ʽ
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // ��������
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // ��������
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // ������������
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }

        // ��������ǰ�˵�:������ʱʱ�䣬���û��೤ʱ�䲻˵��������ʱ����
        mIat.setParameter(SpeechConstant.VAD_BOS,
                mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // ����������˵�:��˵㾲�����ʱ�䣬���û�ֹͣ˵���೤ʱ���ڼ���Ϊ�������룬 �Զ�ֹͣ¼��
        mIat.setParameter(SpeechConstant.VAD_EOS,
                mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // ���ñ�����,����Ϊ"0"���ؽ���ޱ��,����Ϊ"1"���ؽ���б��
        mIat.setParameter(SpeechConstant.ASR_PTT,
                mSharedPreferences.getString("iat_punc_preference", "1"));

        // ������Ƶ����·����������Ƶ��ʽ֧��pcm��wav������·��Ϊsd����ע��WRITE_EXTERNAL_STORAGEȨ��
        // ע��AUDIO_FORMAT���������Ҫ���°汾������Ч
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
                Environment.getExternalStorageDirectory() + "/msc/iat.wav");

        // ������д����Ƿ�����̬������Ϊ��1��������д�����ж�̬�����ط��ؽ��������ֻ����д����֮�󷵻����ս��
        // ע���ò�����ʱֻ��������д��Ч
        mIat.setParameter(SpeechConstant.ASR_DWA,
                mSharedPreferences.getString("iat_dwa_preference", "0"));
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }
}