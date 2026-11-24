package org.telegram.ui.kgram;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.kgram.KgramAuthPolicy;
import org.telegram.messenger.kgram.KgramSession;
import org.telegram.messenger.kgram.OpenIMChatClient;
import org.telegram.messenger.kgram.KgramTgBridge;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

/**
 * kgram register: phone number is required; demo verify code is 666666.
 */
public class KgramRegisterActivity extends FragmentActivity {

    public static final String EXTRA_AREA = "area";
    public static final String EXTRA_PHONE = "phone";

    private EditTextBoldCursor areaField;
    private EditTextBoldCursor phoneField;
    private EditTextBoldCursor codeField;
    private EditTextBoldCursor nickField;
    private EditTextBoldCursor passField;
    private EditTextBoldCursor pass2Field;
    private TextView errorView;
    private RadialProgressView progressView;
    private TextView nextBtn;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        try {
            AndroidUtilities.checkDisplaySize(this, getResources().getConfiguration());
            Theme.createCommonResources(this);
        } catch (Throwable ignore) {
        }

        KgramAuthPolicy authPolicy = KgramAuthPolicy.app();
        String areaCode = OpenIMChatClient.normalizeAreaCode(getIntent() != null ? getIntent().getStringExtra(EXTRA_AREA) : null);
        String phone = OpenIMChatClient.normalizePhone(getIntent() != null ? getIntent().getStringExtra(EXTRA_PHONE) : null);
        if (TextUtils.isEmpty(areaCode)) {
            areaCode = authPolicy.defaultAreaCode();
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        ActionBar actionBar = new ActionBar(this);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(authPolicy.registerPageTitle());
        actionBar.setOccupyStatusBar(true);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        actionBar.setCastShadows(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    openLogin();
                }
            }
        });
        root.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(24));

        TextView title = new TextView(this);
        title.setText("创建账号");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        content.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView sub = new TextView(this);
        sub.setText("注册需要填写手机号。");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        content.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        content.addView(label("手机号"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        LinearLayout phoneRow = new LinearLayout(this);
        phoneRow.setOrientation(LinearLayout.HORIZONTAL);
        areaField = field(false);
        areaField.setHint("+86");
        areaField.setInputType(InputType.TYPE_CLASS_PHONE);
        areaField.setText(areaCode);
        phoneRow.addView(areaField, LayoutHelper.createLinear(88, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        phoneField = field(false);
        phoneField.setHint(authPolicy.registerPhoneHint());
        phoneField.setInputType(InputType.TYPE_CLASS_PHONE);
        if (!TextUtils.isEmpty(phone)) {
            phoneField.setText(phone);
        }
        phoneRow.addView(phoneField, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        content.addView(phoneRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 12));

        TextView codeHint = new TextView(this);
        codeHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        codeHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        codeHint.setPadding(0, 0, 0, AndroidUtilities.dp(12));
        if (authPolicy.registerShowsVerifyCode()) {
            content.addView(label("验证码"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            codeField = field(false);
            codeField.setHint("请输入验证码");
            codeField.setInputType(InputType.TYPE_CLASS_NUMBER);
            content.addView(codeField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 4));
            codeHint.setText(authPolicy.registerVerifyCodeHint());
        } else {
            codeHint.setText(authPolicy.registrationUnavailableMessage());
        }
        content.addView(codeHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        content.addView(label("昵称"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        nickField = field(false);
        nickField.setHint("怎么称呼你");
        if (!TextUtils.isEmpty(phone)) {
            nickField.setText("kgram_" + phone);
        }
        content.addView(nickField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 12));

        content.addView(label("设置密码"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        passField = field(true);
        passField.setHint("至少 6 位");
        content.addView(passField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 12));

        content.addView(label("确认密码"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        pass2Field = field(true);
        pass2Field.setHint("再输入一次");
        pass2Field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pass2Field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doRegister();
                return true;
            }
            return false;
        });
        content.addView(pass2Field, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        errorView = new TextView(this);
        errorView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        errorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        errorView.setVisibility(android.view.View.GONE);
        errorView.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        content.addView(errorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        nextBtn = new TextView(this);
        nextBtn.setText("注册并进入");
        nextBtn.setGravity(Gravity.CENTER);
        nextBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nextBtn.setTypeface(AndroidUtilities.bold());
        nextBtn.setTextColor(0xffffffff);
        nextBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));
        nextBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        nextBtn.setOnClickListener(v -> doRegister());
        content.addView(nextBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 28, 0, 0));

        TextView goLogin = new TextView(this);
        goLogin.setText("已有账号？去登录");
        goLogin.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
        goLogin.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        goLogin.setPadding(0, AndroidUtilities.dp(18), 0, 0);
        goLogin.setOnClickListener(v -> openLogin());
        content.addView(goLogin, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scroll.addView(content);
        int top = ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight;
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, top, 0, 0));

        progressView = new RadialProgressView(this);
        progressView.setVisibility(android.view.View.GONE);
        root.addView(progressView, LayoutHelper.createFrame(40, 40, Gravity.CENTER));

        setContentView(root);
        if (!authPolicy.isRegistrationAvailable()) {
            if (areaField != null) {
                areaField.setEnabled(false);
            }
            if (phoneField != null) {
                phoneField.setEnabled(false);
            }
            if (nickField != null) {
                nickField.setEnabled(false);
            }
            if (passField != null) {
                passField.setEnabled(false);
            }
            if (pass2Field != null) {
                pass2Field.setEnabled(false);
            }
            if (nextBtn != null) {
                nextBtn.setEnabled(false);
                nextBtn.setAlpha(0.5f);
                nextBtn.setText("暂不能注册");
            }
        }
    }

    private String currentArea() {
        return OpenIMChatClient.normalizeAreaCode(areaField != null && areaField.getText() != null
                ? areaField.getText().toString() : "");
    }

    private String currentPhone() {
        return OpenIMChatClient.normalizePhone(phoneField != null && phoneField.getText() != null
                ? phoneField.getText().toString() : "");
    }

    private void openLogin() {
        Intent i = new Intent(this, KgramLoginActivity.class);
        String area = currentArea();
        String phone = currentPhone();
        if (!TextUtils.isEmpty(area) && !TextUtils.isEmpty(phone)) {
            i.putExtra(KgramLoginActivity.EXTRA_AREA, area);
            i.putExtra(KgramLoginActivity.EXTRA_PHONE, phone);
        }
        startActivity(i);
        finish();
    }

    private TextView label(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        v.setTypeface(AndroidUtilities.bold());
        v.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        return v;
    }

    private EditTextBoldCursor field(boolean password) {
        EditTextBoldCursor e = new EditTextBoldCursor(this);
        e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        e.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        e.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        e.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        e.setCursorSize(AndroidUtilities.dp(20));
        e.setCursorWidth(1.5f);
        e.setBackgroundDrawable(Theme.createEditTextDrawable(this, false));
        e.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(12), AndroidUtilities.dp(2), AndroidUtilities.dp(12));
        e.setSingleLine(true);
        e.setMinHeight(AndroidUtilities.dp(48));
        if (password) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        return e;
    }

    private void clearError() {
        if (errorView != null) {
            errorView.setVisibility(android.view.View.GONE);
            errorView.setText("");
        }
    }

    private void showFieldError(String msg) {
        if (errorView != null) {
            errorView.setText(msg);
            errorView.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void doRegister() {
        if (busy) {
            return;
        }
        clearError();
        KgramAuthPolicy policy = KgramAuthPolicy.app();
        if (!policy.isRegistrationAvailable()) {
            showFieldError(policy.registrationUnavailableMessage());
            return;
        }
        String areaCode = currentArea();
        String phone = currentPhone();
        if (!policy.isValidPhone(areaCode, phone)) {
            showFieldError("请填写正确的国家区号和手机号");
            return;
        }
        String entered = (codeField != null && codeField.getText() != null) ? codeField.getText().toString().trim() : "";
        String code = policy.resolveRegisterVerifyCode(entered);
        if (code == null) {
            showFieldError(policy.invalidVerifyCodeMessage());
            return;
        }
        String nick = nickField.getText() != null ? nickField.getText().toString().trim() : "";
        String password = passField.getText() != null ? passField.getText().toString() : "";
        String password2 = pass2Field.getText() != null ? pass2Field.getText().toString() : "";
        if (nick.length() == 0) {
            showFieldError("请填写昵称");
            return;
        }
        if (password.length() < 6) {
            showFieldError("密码至少 6 位");
            return;
        }
        if (!password.equals(password2)) {
            showFieldError("两次密码不一致");
            return;
        }
        busy = true;
        progressView.setVisibility(android.view.View.VISIBLE);
        nextBtn.setEnabled(false);
        nextBtn.setAlpha(0.5f);
        final String submitArea = areaCode;
        final String submitPhone = phone;
        OpenIMChatClient.registerAsync(submitArea, submitPhone, password, nick, code, result -> AndroidUtilities.runOnUIThread(() -> {
            if (result.ok()) {
                finishAuth(submitArea, submitPhone, nick, result);
                return;
            }
            if (result.alreadyRegistered()) {
                OpenIMChatClient.loginAsync(submitArea, submitPhone, password, result2 -> AndroidUtilities.runOnUIThread(() -> {
                    if (result2.ok()) {
                        String useNick = !TextUtils.isEmpty(result2.nickname) ? result2.nickname : nick;
                        finishAuth(submitArea, submitPhone, useNick, result2);
                        return;
                    }
                    busy = false;
                    progressView.setVisibility(android.view.View.GONE);
                    nextBtn.setEnabled(true);
                    nextBtn.setAlpha(1f);
                    if (result2.passwordError()) {
                        new AlertDialog.Builder(this)
                                .setTitle("账号已注册")
                                .setMessage("该手机号已有账号，但密码不正确。")
                                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                                .setPositiveButton("去登录", (d, w) -> openLogin())
                                .show();
                    } else {
                        showFieldError(OpenIMChatClient.friendlyMessage(result2));
                    }
                }));
                return;
            }
            busy = false;
            progressView.setVisibility(android.view.View.GONE);
            nextBtn.setEnabled(true);
            nextBtn.setAlpha(1f);
            showFieldError(OpenIMChatClient.friendlyMessage(result));
        }));
    }

    private void finishAuth(String areaCode, String phone, String nick, OpenIMChatClient.Result result) {
        busy = false;
        progressView.setVisibility(android.view.View.GONE);
        nextBtn.setEnabled(true);
        nextBtn.setAlpha(1f);
        if (TextUtils.isEmpty(result.userID) || TextUtils.isEmpty(result.imToken)) {
            alert("注册异常", "服务器未返回 userID / imToken");
            return;
        }
        String saveNick = !TextUtils.isEmpty(result.nickname) ? result.nickname : nick;
        KgramSession.save(this, areaCode, phone, saveNick, result.userID, result.imToken, result.chatToken);
        KgramTgBridge.activateFromSession(this);
        Intent i = new Intent(this, LaunchActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void alert(String title, String msg) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton(LocaleController.getString(R.string.OK), null).show();
    }
}
