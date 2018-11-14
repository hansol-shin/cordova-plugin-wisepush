package kr.co.itsm.plugin;

import android.os.Message;
import android.util.Log;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import smartro.co.kr.main.SMTCatLinkageModuleMain;
import smartro.co.kr.protocol.SMTMsgDataMap;
import smartro.co.kr.transdirvermodule.SMTTransDriverMain.OnTransCallback;
import smartro.co.kr.util.SMTCommon;

public class SMTCAT extends CordovaPlugin {
    private static final String TAG = "SMTCAT";
    private int orderNo = 1;

    private CordovaInterface cordova;
  CallbackContext connectCallback = null;
  CallbackContext disconnectCallback = null;
  CallbackContext tradeCallback = null;
  CallbackContext tradeCancelCallback = null;
  CallbackContext printCallback = null;

  private static int TRADE_APP = 1;		// 거래 승인
  private static int TRADE_APP_CAN = 2;	// 거래 승인 취소
  private static int TRADE_CAT_CAN = 3;	// 단말기 거래 요청 취소
  private static int TRADE_LINKED_CONFIRM = 4;	// 연결 확인
  private static int TRADE_PAPER_MONEY_IN = 5;	// 수표 조회

  private boolean mIsConnect = false;

  private static SMTCatLinkageModuleMain mpModule = null;

	public static CordovaWebView gWebView;

    public SMTCAT() {}

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
		    gWebView = webView;

        mpModule = new SMTCatLinkageModuleMain(cordova.getActivity(), 3, new SMTTransCallback(), false);

        Log.d(TAG, "==> SMTCAT initialize");
    }

  @Override
  public void onDestroy() {
      if (mpModule != null)
        mpModule.SMTDeviceDisConnect();
  }

    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG,"==> SMTCAT execute: "+ action);

        try{
          // READY //
          if (action.equals("ready")) {
            //
            callbackContext.success();
          }
          // VERIFY CONNECTION //
          else if (action.equals("isConnected")) {
              cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                  if (mIsConnect) {
                    int nRet = 0;
                    nRet = TradeMainProc(TRADE_LINKED_CONFIRM, 0, 0, 0, null, null, null);
                    if (nRet < 0) {
                      Log.d(TAG,"==> SMTCAT isConnected: false");
                      callbackContext.success("false");
                      mIsConnect = false;
                    } else {
                      Log.d(TAG,"==> SMTCAT isConnected: true");
                      callbackContext.success("true");
                    }
                  } else {
                    Log.d(TAG,"==> SMTCAT isConnected: false");
                    callbackContext.success("false");
                  }
                }
              });
          }
          else if (action.equals("connect")) {
            connectCallback = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                try {
                  int nRet = ConnectSync(args.getString(0), args.getInt(1));
                  if (nRet < 1)
                    callbackContext.error(0);
                } catch (Exception e) {
                  callbackContext.error(e.toString());
                }
              }
            });
          }
          else if (action.equals("disconnect")) {
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                disconnectCallback = callbackContext;
                if (mpModule != null) {
                  mpModule.SMTDeviceDisConnect();
                  mpModule = null;
                } else {
                  callbackContext.success();
                }
              }
            });
          }
          else if (action.equals("trade")) {
            tradeCallback = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                try {
                  int tradeType =  args.getInt(0);
                  int amt =  args.getInt(1);
                  int instalment = args.getInt(2);
                  JSONArray arr = args.getJSONArray(3);
                  int nRet = TradeMainProc(TRADE_APP, tradeType, amt, instalment, arr, null, null);
                  if (nRet < 0) {
                    callbackContext.error("결제단말기 통신오류");
                  }
                } catch (Exception e) {
                  callbackContext.error("시스템 오류");
                }
              }
            });
          }
          else if (action.equals("tradeCancel")) {
            tradeCancelCallback = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                try {
                  int tradeType =  args.getInt(0);
                  int amt =  args.getInt(1);
                  int instalment = args.getInt(2);
                  String approvalNo = args.getString(3);
                  String approvalDate = args.getString(3);
                  int nRet = TradeMainProc(TRADE_APP_CAN, tradeType, amt, instalment, null, approvalNo, approvalDate);
                  if (nRet < 0) {
                    callbackContext.error(nRet);
                  }
                } catch (Exception e) {
                  callbackContext.error(0);
                }
              }
            });
          }
          else if (action.equals("cancel")) {
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                try {
                  int nRet = mpModule.SMTSendData(SMTCommon.MSG_TYPE_NAME, "99999", new HashMap<String, String>());
//                  int nRet = 1;
                  if (nRet > 0) {
                    callbackContext.success( "OK" );
                  } else {
                    callbackContext.error(nRet);
                  }
                } catch (Exception e) {
                  callbackContext.error(0);
                }
              }
            });
          }
          else if (action.equals("print")) {
            printCallback = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
              @Override
              public void run() {
                try {
                  JSONArray arr =  args.getJSONArray(0);

                  HashMap<String, String> SndData = new HashMap<String, String>();
                  String strData = SpecialPrintData(arr);

                  SndData.put(SMTMsgDataMap.MSG_TYPE_REQUEST_CODE.Name, "A1");
                  SndData.put(SMTMsgDataMap.MSG_TYPE_OUTPUT_DATA.Name, strData);

                  TradeMain("특수출력", "5301", SndData);
                } catch (Exception e) {
                  callbackContext.error(0);
                }
              }
            });
          }
          else if (action.equals("setOrderNo")) {
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                try {
                  int no = args.getInt(0);
                  orderNo = no;
                  callbackContext.success();
                } catch (Exception e) {
                  callbackContext.error(e.toString());
                }
              }
            });
          }
          else{
            callbackContext.error("Method not found");
            return false;
          }
        }catch(Exception e){
          Log.d(TAG, "ERROR: onPluginAction: " + e.getMessage());
          callbackContext.error(e.getMessage());
          return false;
        }

        return true;
    }

    private void SMTLOG(String strData) {
      Log.d(TAG, strData);
    }

  private int ConnectSync(String ip, int port)
  {
    int nRet = 0;
    String strMSG;

    if (mpModule == null) {
      mpModule = new SMTCatLinkageModuleMain(cordova.getActivity(), 3, new SMTTransCallback(), false);
    }

    mpModule.SMTSetIPPort(ip, port);

    // Permission 요청
    mpModule.SMTPermission();

    // Connect
    nRet = mpModule.SMTDeviceConnect();
    if(nRet < 0)
    {
      strMSG = String.format(Locale.KOREAN, "Connect Error !!!(%d)\n", nRet);
    }
    else
    {
      strMSG = String.format("Connect OK\n");
    }

    SMTLOG(strMSG);

    return nRet;
  }

  // Callback
  class SMTTransCallback implements OnTransCallback {

    public void OnTransEvent(Message msg) {
      // TODO Auto-generated method stub
      int nType = msg.what;
      int nResult = msg.arg1;
      String strMSG = new String();

      if(nResult > 0)
      {
        switch(nType)
        {
          case SMTCommon.EVT_CONNECT:
          {
            SMTLOG(String.format(Locale.KOREAN, "[EVENT] CONNECT (%d)!!!", nResult));
            connectCallback.success( "Connected" );
            mIsConnect = true;
            break;
          }
          case SMTCommon.EVT_SEND:
          {
            SMTLOG(String.format(Locale.KOREAN, "[EVENT] SEND (%d)!!!", nResult));
            break;
          }
          case SMTCommon.EVT_RECEIVE:
          {
            SMTLOG(String.format(Locale.KOREAN, "[EVENT] RECEIVE (%d)!!!", nResult));
            LinkedHashMap<String, String> mOutData = (LinkedHashMap<String, String>)msg.obj;

            if(mOutData == null)
            {
              strMSG = String.format("RCV ERROR!!! (%s)", "NULL");
            }
            else if(mOutData.size() == 0)
            {
              strMSG = String.format("RCV ERROR!!! (%s)", "SIZE ZERO");
            }
            else
            {
              int i = 0;
              Object [] keys = mOutData.keySet().toArray();

              String serviceId = null;
              String cardID = null;
              String approvalNo = null;
              String approvalDate = null;
              String message = "";
              String issueName = "";
              String code = "";

              for(Object key : keys)
              {
                String value = mOutData.get(key);

                if(i == 0)	strMSG = String.format("[%s] <%s>\n", key, value);
                else 		strMSG += String.format("[%s] <%s>\n", key, value);

                // 서비스 유형
                if(key.equals(SMTMsgDataMap.MSG_TYPE_SERVICE_TYPE.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_SERVICE_TYPE.Number) == true)
                {
                  serviceId = value;
                  Log.d(TAG, "서비스코드: " + value);
                }

                // 승인 번호
                if(key.equals(SMTMsgDataMap.MSG_TYPE_APPROVAL_ID.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_APPROVAL_ID.Number) == true)
                {
                  approvalNo = value;
                }

                // 승인 일자
                if(key.equals(SMTMsgDataMap.MSG_TYPE_SALES_DATE.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_SALES_DATE.Number) == true)
                {
                  approvalDate = value;
                }

                // 화면 메시지
                if(key.equals(SMTMsgDataMap.MSG_TYPE_DISPLAY_MSG.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_DISPLAY_MSG.Number) == true)
                {
                  message = value;
                }

                // 응답코드
                if(key.equals(SMTMsgDataMap.MSG_TYPE_RESPONSE_CODE.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_RESPONSE_CODE.Number) == true)
                {
                  code = value;
                }

                // 카드번호
                if(key.equals(SMTMsgDataMap.MSG_TYPE_CARD_NUMBER.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_CARD_NUMBER.Number) == true)
                {
                  cardID = value;
                }

                // 발급사
                if(key.equals(SMTMsgDataMap.MSG_TYPE_ISSUE_NAME.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_ISSUE_NAME.Number) == true)
                {
                  issueName = value;
                }

                i++;
              }

              if (serviceId.equals("0102")) {
                if (tradeCallback != null) {
                  if (approvalNo != null && !approvalNo.equals("") && code.equals("00")) {
                    try {
                      JSONObject res = new JSONObject();
                      res.put("orderNo", orderNo++);
                      res.put("cardID", cardID);
                      res.put("approvalNo", approvalNo);
                      res.put("approvalDate", approvalDate);
                      res.put("issueName", issueName);

                      tradeCallback.success(res);
                    } catch (Exception e) {
                      tradeCallback.error("JSON 에러!");
                    }
                  } else {
                    tradeCallback.error(message);
                  }
                }
              } else if (serviceId.equals("2102")) {
                if (tradeCancelCallback != null) {
                  if (code.equals("00")) {
                    tradeCancelCallback.success();
                  } else {
                    tradeCancelCallback.error(message);
                  }
                }
              }
            }
            SMTLOG(strMSG);

            break;
          }
          case SMTCommon.EVT_DISCONNECT:
          {
            SMTLOG(String.format(Locale.KOREAN, "[EVENT] DISCONNECT (%d)!!!", nResult));
            if (disconnectCallback != null)
              disconnectCallback.success();
            mIsConnect = false;
            break;
          }
          default:
          {
            SMTLOG(String.format(Locale.KOREAN, "[EVENT] [%d] DEFAULT TYPE !!!![%d]", nType, nResult));
            break;
          }
        }
      }
      else
      {
        //mpModule.SMTDeviceDisConnect();
        connectCallback.error(String.format(Locale.KOREAN, "[EVENT] [%d] ERROR !!!![%d]", nType, nResult));
        SMTLOG(String.format(Locale.KOREAN, "[EVENT] [%d] ERROR !!!![%d]", nType, nResult));
      }
    }

  }

  private int TradeMainProc(int nType, int type, int amt, int instalment, JSONArray arr, String approvalNo, String approvalDttm)
  {
    int nRet = 0;
    HashMap<String, String> mapData = new HashMap<String, String>();
    String strTmp = new String();
    String strServiceType = new String();
    int nTradeType = 0;

    if(mpModule == null)
    {
      return -1;
    }

    SMTLOG(String.format(Locale.KOREAN, "[TradeMainProc] Trade TYPE(%d)!!!", nType));

    if(nType == TRADE_CAT_CAN)
    {
      strServiceType = "99999";
    }
    else if(nType == TRADE_LINKED_CONFIRM)
    {
      strServiceType = "9001";
    }
    else if(nType == TRADE_PAPER_MONEY_IN)
    {
      strServiceType = "0401";
    }
    else if(nType == TRADE_APP || nType == TRADE_APP_CAN)
    {
      // 거래 구분 코드
      nTradeType = type;
      if(nTradeType == 0)			strTmp = "01";	// 신용 승인
      else if(nTradeType == 1)	strTmp = "02";	// 현금 승인
      else if(nTradeType == 2)	strTmp = "03";	// 은련 승인(MSR)
      Log.d(TAG, String.format("nTrade TYPe (%d)", nTradeType));
      if(strTmp != null)
      {
        mapData.put(SMTMsgDataMap.MSG_TYPE_TRADE_SEPARATE_CODE.Name, strTmp);
      }

      // 승인 금액
      strTmp = ""+amt;
      if(strTmp != null)
      {
        mapData.put(SMTMsgDataMap.MSG_TYPE_APPROVAL_AMOUNT.Name, strTmp);
      }

      // 세금
      strTmp = null;
      if(strTmp != null)
      {
        mapData.put(SMTMsgDataMap.MSG_TYPE_TAX.Name, strTmp);
      }

      // 봉사료
      strTmp = null;
      if(strTmp != null)
      {
        mapData.put(SMTMsgDataMap.MSG_TYPE_SERVICE_CHARGE.Name, strTmp);
      }

      // 할부
      strTmp = ""+instalment;
      if(strTmp != null)
      {
        mapData.put(SMTMsgDataMap.MSG_TYPE_INSTALLMENT_PERIOD_USED_CHECK.Name, strTmp);
      }

      if (nType == TRADE_APP && arr.length() > 0) {
        String strPrintData = new String();
        byte [] bysrc = new byte[1];
        byte [] byEnd = new byte[1];
        byEnd[0] = (byte)0x0A;
        byte[] magStart = new byte[1];
        magStart[0] = (byte)0x17;
        byte[] magEnd = new byte[1];
        magEnd[0] = (byte)0x18;

        strPrintData = " " + ConvertProtocolbyte(byEnd, 1) + " " + ConvertProtocolbyte(byEnd, 1);
        strPrintData += ConvertProtocolbyte(magStart, 1) + "주문번호를 확인하세요" + ConvertProtocolbyte(byEnd, 1);
        strPrintData += " " + ConvertProtocolbyte(byEnd, 1);
        strPrintData += String.format("   주문번호   %3d", orderNo) + ConvertProtocolbyte(magEnd, 1) + ConvertProtocolbyte(byEnd, 1);
        strPrintData += " " + ConvertProtocolbyte(byEnd, 1) + " " + ConvertProtocolbyte(byEnd, 1);

        strPrintData += "-----------------------------------------------" + ConvertProtocolbyte(byEnd, 1);
        strPrintData += "순번    상품명               수량       금액    " + ConvertProtocolbyte(byEnd, 1);
        strPrintData += "-----------------------------------------------" + ConvertProtocolbyte(byEnd, 1);
        try {
          for (int i = 0; i < arr.length(); i++) {
            JSONObject rec = arr.getJSONObject(i);
            int cnt = rec.getInt("cnt");
            JSONObject menu = rec.getJSONObject("menu");
            int _amt = menu.getInt("saleAmt");
            String name = menu.getJSONObject("name").getString("ko");
            JSONArray opts = rec.getJSONArray("opts");
            for (int j = 0; j < opts.length(); j++) {
              JSONObject opt = opts.getJSONObject(j);
              _amt += opt.getInt("amt");
            }

            strPrintData += String.format("  %,d     %s  %,d개  %s원", i+1, name, cnt, _amt) + ConvertProtocolbyte(byEnd, 1);
            strPrintData += "-----------------------------------------------" + ConvertProtocolbyte(byEnd, 1);
          }
        } catch (Exception e) {

        }

        mapData.put(SMTMsgDataMap.MSG_TYPE_ITEM_NAME.Name, strPrintData);
      }

      // 서명 여부
      mapData.put(SMTMsgDataMap.MSG_TYPE_SIGN_SET.Name, "3");

      if(nType == TRADE_APP_CAN)
      {
        // 승인 번호
        strTmp = approvalNo;
        if(strTmp != null)
        {
          mapData.put(SMTMsgDataMap.MSG_TYPE_APPROVAL_ID.Name, strTmp);
        }

        // 승인 일자
        strTmp = approvalDttm;
        if(strTmp != null)
        {
          mapData.put(SMTMsgDataMap.MSG_TYPE_BASE_TRADE_DATE.Name, strTmp);
        }

        if(nTradeType == 1)
        {
          mapData.put(SMTMsgDataMap.MSG_TYPE_CASH_CANCEL_REASON.Name, "1");
        }



        strServiceType = "2101";
      }
      else if (nType == TRADE_APP)
      {
        strServiceType = "0101";
      }

      String mFillerData = null;
      if(mFillerData != null)
      {
        mapData.put(SMTMsgDataMap.MSG_TYPE_FILLER1.Name, mFillerData);
      }
    }

    nRet = mpModule.SMTSendData(SMTCommon.MSG_TYPE_NAME, strServiceType, mapData);

    return nRet;
  }

  private void TradeMain(String strTitle, String strServiceCode, HashMap<String, String> SndData)
  {
    int nRet = 0;
    String strMSG = new String();

    SMTLOG(String.format("[%s]", strTitle));

    if(mIsConnect == true)
    {
      if(strServiceCode.compareTo("5501") == 0)	// 응답 없음.
      {
        nRet = mpModule.SMTSendNoResponse(true, strServiceCode, 0, SndData);
      }
      else
      {
        nRet = mpModule.SMTSendData(SMTCommon.MSG_TYPE_NAME, strServiceCode, SndData);
      }

      strMSG = String.format(Locale.KOREAN, "[%s] SEND (%d)", strTitle, nRet);
    }
    else
    {
      strMSG = String.format(Locale.KOREAN, "[%s] Not Open", strTitle);
    }

    SMTLOG(strMSG);

    if(true)
    {
      if(nRet > 0)
      {
        LinkedHashMap<String, String> mOutData = new LinkedHashMap<String, String>();

        nRet = mpModule.SMTRcvData(mOutData, 50);

        if(nRet > 0)
        {
          if(mOutData == null)
          {
            strMSG = String.format(Locale.KOREAN, "[%s] RCV ERROR!!! (%s)", strTitle, "NULL");
            printCallback.error(0);
          }
          else if(mOutData.size() == 0)
          {
            strMSG = String.format("[%s] RCV ERROR!!! (%s)", strTitle, "SIZE ZERO");
            printCallback.error(0);
          }
          else
          {
            int i = 0;
            Object [] keys = mOutData.keySet().toArray();
            for(Object key : keys)
            {
              String value = mOutData.get(key);

              if(i == 0)	strMSG = String.format("==== [%s] ====\n[%s] <%s>\n", strTitle, key, value);
              else 		strMSG += String.format("[%s] <%s>\n", key, value);

              // 승인 번호
              if(key.equals(SMTMsgDataMap.MSG_TYPE_APPROVAL_ID.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_APPROVAL_ID.Number) == true)
              {
                //edAppNum.setText(value);
              }

              // 승인 일자
              if(key.equals(SMTMsgDataMap.MSG_TYPE_SALES_DATE.Name) == true || key.equals(SMTMsgDataMap.MSG_TYPE_SALES_DATE.Number) == true)
              {
                //edAppDate.setText(value);
              }

              i++;
            }

            printCallback.success(orderNo);
          }
          SMTLOG(strMSG);
        } else {
          printCallback.error(0);
        }
      }
    }
  }

  private String SpecialPrintData(JSONArray arr)
  {
    String strPrintData = new String();
    String strOutData = new String();
    byte [] bysrc = new byte[1];
    byte [] byEnd = new byte[1];

    byEnd[0] = (byte)0x0A;

    strPrintData += ConvertProtocolbyte(byEnd, 1);
    strPrintData += ConvertProtocolbyte(byEnd, 1);
    strPrintData += ConvertProtocolbyte(byEnd, 1);

    strPrintData += "-----------------------------------------------" + ConvertProtocolbyte(byEnd, 1);
    for (int i = 0; i < arr.length(); i++) {
      try {
        bysrc[0] = 0x17;
        strPrintData = ConvertProtocolbyte(bysrc, 1);
        strPrintData += arr.getString(i);
        bysrc[0] = 0x18;
        strPrintData += ConvertProtocolbyte(bysrc, 1) + ConvertProtocolbyte(byEnd, 1);
      } catch (JSONException e) {

      }
    }
    strPrintData += "-----------------------------------------------" + ConvertProtocolbyte(byEnd, 1);

    strPrintData += ConvertProtocolbyte(byEnd, 1);
    strPrintData += ConvertProtocolbyte(byEnd, 1);
    strPrintData += ConvertProtocolbyte(byEnd, 1);

    bysrc[0] = 0x1D;
    strPrintData += ConvertProtocolbyte(bysrc, 1);
    strPrintData += ConvertProtocolbyte(bysrc, 1);
    strPrintData += ConvertProtocolbyte(bysrc, 1);

    try {
      byte [] byLength = strPrintData.getBytes("KSC5601");

      strOutData = String.format("%04d", byLength.length) + strPrintData;

      Log.d(TAG, strOutData);
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();

      return null;
    }


    return strOutData;
  }

  private String ConvertProtocolbyte(byte [] src, int nLen)
  {
    String strOut = new String(src);

    return strOut;
  }
}
