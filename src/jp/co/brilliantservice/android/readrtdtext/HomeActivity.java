/*
 * Copyright 2012 yamashita@brilliantservice.co.jp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.brilliantservice.android.readrtdtext;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Toast;

/**
 * @author yamashita@brilliantservice.co.jp
 */
public class HomeActivity extends Activity {

    public static final String LOG_TAG = HomeActivity.class.getSimpleName();

    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(getApplicationContext(), "not found NFC feature", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "NFC feature is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0);
        IntentFilter[] intentFilter = new IntentFilter[] {
            new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        };
        String[][] techList = new String[][] {
            {
                android.nfc.tech.Ndef.class.getName()
            }
        };
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, techList);

    }

    @Override
    public void onPause() {
        super.onPause();

        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            Toast.makeText(getApplicationContext(), "null action", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!action.equals(NfcAdapter.ACTION_TECH_DISCOVERED))
            return;

        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages == null) {
            Toast.makeText(getApplicationContext(), "Null Message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (messages.length == 0) {
            Toast.makeText(getApplicationContext(), "Empty Message", Toast.LENGTH_SHORT).show();
            return;
        }

        NdefRecord[] records = ((NdefMessage)messages[0]).getRecords();
        if (records == null || records.length == 0) {
            Toast.makeText(getApplicationContext(), "Empty Record", Toast.LENGTH_SHORT).show();
            return;
        }

        for (NdefRecord record : records) {
            if (isTextRecord(record)) {
                String textAndLangCode = getTextAndLangCode(record);
                Toast.makeText(getApplicationContext(), textAndLangCode, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Not Text Record", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * NdefRecordがRTDのTextか判定します
     * 
     * @param record
     * @return true:RTD Text Recotd , false:not RTD Text Recotd
     */
    private boolean isTextRecord(NdefRecord record) {
        return record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT);
    }

    /**
     * RTD Text Recordから文字列と言語コードを取得します
     * 
     * @param record
     * @return テキスト(言語コード)
     */
    private String getTextAndLangCode(NdefRecord record) {
        if (record == null)
            throw new IllegalArgumentException();

        byte[] payload = record.getPayload();
        byte flags = payload[0];
        String encoding = ((flags & 0x80) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = flags & 0x3F;
        try {
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            String text = new String(payload, 1 + languageCodeLength, payload.length
                    - (1 + languageCodeLength), encoding);

            return String.format("%s(%s)", text, languageCode);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }
}
