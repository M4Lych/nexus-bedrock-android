package org.levimc.launcher.ui.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.levimc.launcher.R;
import org.levimc.launcher.core.ai.AICapability;
import org.levimc.launcher.core.ai.AIConversation;
import org.levimc.launcher.core.ai.AIError;
import org.levimc.launcher.core.ai.AIMessage;
import org.levimc.launcher.core.ai.AIProvider;
import org.levimc.launcher.core.ai.AIRequest;
import org.levimc.launcher.core.ai.AIResponse;
import org.levimc.launcher.core.ai.AIService;
import org.levimc.launcher.core.ai.UnavailableAIProvider;

import java.util.List;

/** Session-only assistant UI. No backend is installed in this application build. */
public class AIAssistantActivity extends BaseActivity {
    private static final String AI_PREFERENCES = "ai_assistant_privacy";
    private static final String KEY_EXTERNAL_REQUESTS_OPT_IN = "external_requests_opt_in";

    private final AIConversation conversation = new AIConversation();
    private AIService aiService;
    private SharedPreferences privacyPreferences;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;
    private TextView emptyState;
    private TextView errorState;
    private EditText messageInput;
    private MaterialButton sendButton;
    private SwitchMaterial optInSwitch;
    private ProgressBar loading;
    private TextView loadingLabel;
    private boolean suppressOptInListener;
    private boolean requestInProgress;
    private int requestGeneration;

    private static final AICapability[] CAPABILITIES = {
            AICapability.ADDON_DISCOVERY,
            AICapability.CONTENT_EXPLANATION,
            AICapability.COMPATIBILITY_ASSISTANCE,
            AICapability.TROUBLESHOOTING
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        // Intentionally unavailable until an authorized provider is composed into the app.
        aiService = new AIService(new UnavailableAIProvider());
        privacyPreferences = getSharedPreferences(AI_PREFERENCES, Context.MODE_PRIVATE);

        messagesContainer = findViewById(R.id.ai_messages_container);
        messagesScroll = findViewById(R.id.ai_messages_scroll);
        emptyState = findViewById(R.id.ai_empty_state);
        errorState = findViewById(R.id.ai_error_state);
        messageInput = findViewById(R.id.ai_message_input);
        sendButton = findViewById(R.id.ai_send_button);
        optInSwitch = findViewById(R.id.ai_opt_in_switch);
        loading = findViewById(R.id.ai_loading);
        loadingLabel = findViewById(R.id.ai_loading_label);
        Spinner capabilitySpinner = findViewById(R.id.ai_capability_spinner);
        MaterialButton clearButton = findViewById(R.id.ai_clear_button);

        android.widget.ArrayAdapter<String> capabilityAdapter = new android.widget.ArrayAdapter<>(this,
                R.layout.spinner_item, getCapabilityLabels());
        capabilityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        capabilitySpinner.setAdapter(capabilityAdapter);

        suppressOptInListener = true;
        optInSwitch.setChecked(privacyPreferences.getBoolean(KEY_EXTERNAL_REQUESTS_OPT_IN, false));
        suppressOptInListener = false;
        optInSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (suppressOptInListener) return;
            if (checked) {
                suppressOptInListener = true;
                button.setChecked(false);
                suppressOptInListener = false;
                showOptInNotice();
            } else {
                privacyPreferences.edit().putBoolean(KEY_EXTERNAL_REQUESTS_OPT_IN, false).apply();
                cancelAndClearConversation();
            }
        });

        sendButton.setOnClickListener(view -> sendMessage(capabilitySpinner.getSelectedItemPosition()));
        messageInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(capabilitySpinner.getSelectedItemPosition());
                return true;
            }
            return false;
        });
        clearButton.setOnClickListener(view -> cancelAndClearConversation());
        renderConversation();
    }

    private String[] getCapabilityLabels() {
        return new String[]{
                getString(R.string.ai_capability_discovery),
                getString(R.string.ai_capability_explanation),
                getString(R.string.ai_capability_compatibility),
                getString(R.string.ai_capability_troubleshooting)
        };
    }

    private void showOptInNotice() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_opt_in_dialog_title)
                .setMessage(R.string.ai_opt_in_dialog_message)
                .setNegativeButton(R.string.ai_opt_in_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.ai_opt_in_confirm, (dialog, which) -> {
                    privacyPreferences.edit().putBoolean(KEY_EXTERNAL_REQUESTS_OPT_IN, true).apply();
                    suppressOptInListener = true;
                    optInSwitch.setChecked(true);
                    suppressOptInListener = false;
                })
                .show();
    }

    private void sendMessage(int capabilityIndex) {
        if (requestInProgress) return;
        errorState.setVisibility(View.GONE);
        String text = messageInput.getText() == null ? "" : messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        if (aiService.getAvailability() != org.levimc.launcher.core.ai.AIAvailability.AVAILABLE) {
            showError(AIError.NOT_CONFIGURED);
            return;
        }
        if (!privacyPreferences.getBoolean(KEY_EXTERNAL_REQUESTS_OPT_IN, false)) {
            showError(AIError.OPT_IN_REQUIRED);
            return;
        }

        try {
            conversation.add(new AIMessage(AIMessage.Role.USER, text));
            AICapability capability = CAPABILITIES[Math.max(0,
                    Math.min(CAPABILITIES.length - 1, capabilityIndex))];
            AIRequest request = new AIRequest(capability, conversation.snapshot());
            messageInput.setText("");
            renderConversation();
            setLoading(true);
            final int requestId = ++requestGeneration;
            boolean optedIn = privacyPreferences.getBoolean(KEY_EXTERNAL_REQUESTS_OPT_IN, false);
            aiService.generate(request, optedIn, new AIProvider.Callback() {
                @Override
                public void onSuccess(AIResponse response) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed() || requestId != requestGeneration) return;
                        setLoading(false);
                        if (response == null) {
                            showError(AIError.INVALID_RESPONSE);
                            return;
                        }
                        conversation.add(new AIMessage(AIMessage.Role.ASSISTANT, response.getText()));
                        renderConversation();
                    });
                }

                @Override
                public void onError(AIError error) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed() || requestId != requestGeneration) return;
                        setLoading(false);
                        showError(error == null ? AIError.UNAVAILABLE : error);
                    });
                }
            });
        } catch (IllegalArgumentException | IllegalStateException error) {
            showError(AIError.REQUEST_TOO_LARGE);
        }
    }

    private void setLoading(boolean isLoading) {
        requestInProgress = isLoading;
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loadingLabel.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!isLoading);
    }

    private void showError(AIError error) {
        @StringRes int message;
        switch (error) {
            case OPT_IN_REQUIRED: message = R.string.ai_error_opt_in; break;
            case NOT_CONFIGURED: message = R.string.ai_error_not_configured; break;
            case UNAVAILABLE:
            case CANCELLED: message = R.string.ai_error_unavailable; break;
            default: message = R.string.ai_error_generic; break;
        }
        errorState.setText(message);
        errorState.setVisibility(View.VISIBLE);
    }

    private void renderConversation() {
        messagesContainer.removeAllViews();
        List<AIMessage> messages = conversation.snapshot();
        emptyState.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        messagesScroll.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
        for (AIMessage message : messages) {
            TextView text = new TextView(this);
            String speaker = message.getRole() == AIMessage.Role.USER
                    ? getString(R.string.ai_speaker_you) : getString(R.string.ai_speaker_assistant);
            text.setText(speaker + ": " + message.getText());
            text.setTextColor(getColor(R.color.on_surface));
            text.setTextSize(13f);
            text.setTypeface(text.getTypeface(), Typeface.NORMAL);
            text.setTextIsSelectable(true);
            text.setPadding(dp(12), dp(9), dp(12), dp(9));
            text.setBackgroundResource(message.getRole() == AIMessage.Role.USER
                    ? R.drawable.card_background_small : R.drawable.bg_rounded_card);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = message.getRole() == AIMessage.Role.USER ? Gravity.END : Gravity.START;
            params.setMargins(dp(4), 0, dp(4), dp(8));
            messagesContainer.addView(text, params);
        }
        messagesContainer.post(() -> messagesScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void cancelAndClearConversation() {
        requestGeneration++;
        aiService.cancel();
        setLoading(false);
        conversation.clear();
        errorState.setVisibility(View.GONE);
        renderConversation();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (aiService != null) aiService.cancel();
        super.onDestroy();
    }
}
