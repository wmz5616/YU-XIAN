Cypress.Commands.add('login', (username, password) => {
  // 使用 session 缓存登录状态，测试速度快 10 倍
  cy.session([username, password], () => {
    // 直接调后端接口登录，跳过 UI 输入
    cy.request({
      method: 'POST',
      url: '/api/users/login',
      body: { username, password },
    }).then(({ body }) => {
      // 模拟将 Token 存入浏览器
      window.localStorage.setItem('yuxian_token', body.token)
      window.localStorage.setItem('role', body.role)
    })
  })
  // 登录后访问首页，触发状态更新
  cy.visit('/');
})