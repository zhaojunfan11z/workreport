# 任务清单：UI 视觉向 Qoder 对齐（皮肤层）

状态：已完成
当前阶段：验收 / 提交

| # | 任务 | 状态 |
|---|---|---|
| T1 | UiTheme 新增 BUBBLE / SEND / SEND_HOVER 语义色 | 已完成 |
| T2 | UiButton 的 SOLID 支持自定义填充色与 hover 提亮 | 已完成 |
| T3 | UiMetrics 圆角收紧（气泡 10 / 卡片 8 / chip 6 / 按钮 8） | 已完成 |
| T4 | UserMessageView 气泡改中性灰底 + 正文色 | 已完成 |
| T5 | SessionTabsBar 选中态改浅灰底 + 加粗（去底部强调条） | 已完成 |
| T6 | ChatComposer 发送键改绿色圆角方形 | 已完成 |
| T7 | StatusRow meta chip 去底色 | 已完成 |
| T8 | ChatView 块间距 12→10 | 已完成 |
| T9 | 编译 + 重新打包 zip | 已完成 |
| T10 | 追加：助手消息去卡片化（无底色/描边 + 中性角色标签） | 已完成 |
| T11 | 追加：输入框底板改中性灰、去 1px 描边 | 已完成 |
| T12 | 第二阶段：输入区重排（添加上下文入框、模型选择移出、停止/发送入框） | 已完成 |
| T13 | 新增 composer.add.context 中英文案 | 已完成 |

## 执行日志

- 2026-11-16 需求确认：视觉皮肤 / 跟随 IDE 明暗 / 发送键绿，写入 requirements.md
- 2026-11-16 编码完成：11 项全部落地，涉及 9 个 Java 文件；`gradlew classes`、`buildPlugin` 均 BUILD SUCCESSFUL
- 2026-11-16 产物：`build/distributions/workreport-ai-assistant-3.1.1.zip`（15,942,930 字节）
- 2026-11-16 第二阶段完成：输入区拆为框内行（添加上下文 / 停止 / 发送）与框外行（模型 / 自动执行 / 设置）；`chromeHeight` 同步计入新结构；产物更新为 15,942,505 字节（16:14）

## 下一步

分片提交本次改动；用户安装验包后反馈余下差异（变更文件条、头像菜单）。
