package com.workreport.agent.ui.chat;

import com.workreport.agent.AgentBundle;
import com.workreport.agent.ui.design.UiMetrics;
import com.workreport.agent.ui.design.UiTheme;
import com.workreport.agent.ui.markdown.InlineSpan;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Right-aligned user bubble: the typed text plus a muted line listing the attached files.
// Width hugs the text up to 72% of the row, so a one-word question does not paint a huge block.
// 靠右的用户气泡：正文 + 一行浅色附件清单。
// 宽度贴合文本、上限为行宽的 72%，避免一个字的问题铺满整个屏幕。
public final class UserMessageView extends ChatRowView {

    private static final double RATIO = 0.72;

    private final List<InlineLayout> lines = new ArrayList<>();
    private final List<String> attachments = new ArrayList<>();
    private final String plain;

    private final int padX = UiMetrics.scale(12);
    private final int padY = UiMetrics.scale(9);
    private final int lineGap = UiMetrics.scale(2);

    public UserMessageView(String text, List<String> attachmentNames) {
        super(RATIO, true);
        setOpaque(false);

        String body = text == null ? "" : normalize(text);
        for (String line : body.split("\n", -1)) {
            InlineLayout layout = new InlineLayout(List.of(InlineSpan.plain(line.isEmpty() ? " " : line)),
                    UiTheme.font(), false);
            layout.setColorOverride(UiTheme::text);
            lines.add(layout);
        }
        if (attachmentNames != null) {
            attachments.addAll(attachmentNames);
        }
        if (!attachments.isEmpty()) {
            String caption = AgentBundle.message("composer.attachments", String.join("、", attachments));
            InlineLayout files = new InlineLayout(List.of(InlineSpan.plain(caption).withItalic()),
                    UiTheme.font(), false);
            files.setColorOverride(() -> UiTheme.withAlpha(UiTheme.text(), 160));
            lines.add(files);
        }
        plain = flat(body, attachments);
        TextCopy.install(this, () -> plain);
    }

    private static String flat(String body, List<String> attachments) {
        if (attachments.isEmpty()) {
            return body;
        }
        return body + "\n" + AgentBundle.message("composer.attachments", String.join("、", attachments));
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    // Neutral bubble background: Qoder keeps the user message grey and the text in the normal
    // foreground colour, instead of the white-on-accent block this view used to paint.
    // 中性气泡底色：Qoder 的用户消息是灰底正文色，而不是过去的强调色实底白字。
    @Override
    protected int contentWidth(int maxWidth) {
        int textWidth = maxWidth - padX * 2;
        int widest = 0;
        for (InlineLayout layout : lines) {
            layout.measure(Math.max(24, textWidth));
            widest = Math.max(widest, layout.maxAdvance());
        }
        return Math.min(textWidth, widest) + padX * 2;
    }

    @Override
    protected int computeHeight(int width) {
        int textWidth = Math.max(24, width - padX * 2);
        int height = padY * 2;
        for (int i = 0; i < lines.size(); i++) {
            height += lines.get(i).measure(textWidth);
            if (i < lines.size() - 1) {
                height += lineGap;
            }
        }
        return Math.max(height, padY * 2 + UiMetrics.lineHeight(UiTheme.font()));
    }

    @Override
    protected void paintBlock(Graphics2D g2, int width, int height) {
        int arc = UiMetrics.radiusBubble();
        g2.setColor(UiTheme.BUBBLE);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        int textWidth = Math.max(24, width - padX * 2);
        int y = padY;
        for (int i = 0; i < lines.size(); i++) {
            InlineLayout layout = lines.get(i);
            layout.paint(g2, padX, y);
            y += layout.measure(textWidth);
            if (i < lines.size() - 1) {
                y += lineGap;
            }
        }
    }
}
