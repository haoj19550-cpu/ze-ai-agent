<template>
  <main class="home-page">
    <section class="home-shell">
      <div class="home-copy">
        <span class="eyebrow">ZE AI Agent</span>
        <h1>选择一个 AI 应用开始对话</h1>
        <p>两个应用都支持实时流式输出，适合快速测试后端聊天能力和智能体效果。</p>
      </div>

      <div class="app-grid" aria-label="应用列表">
        <RouterLink class="app-card love-card" to="/love">
          <span class="app-icon">AI</span>
          <strong>AI 恋爱大师</strong>
          <span>带独立聊天室 ID 的情感咨询对话。</span>
        </RouterLink>

        <RouterLink class="app-card manus-card" to="/manus">
          <span class="app-icon">AG</span>
          <strong>AI 超级智能体</strong>
          <span>连接智能体接口，实时查看任务执行反馈。</span>
        </RouterLink>
      </div>
    </section>
  </main>
</template>
