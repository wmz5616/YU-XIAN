describe('用户认证系统测试', () => {
  // 修复：生成 6 位随机用户名 (u + 5位数字)，符合 ^[a-z0-9]{1,7}$ 限制
  const randomId = Math.floor(10000 + Math.random() * 90000);
  const username = `u${randomId}`; 
  const password = 'password123';

  it('游客应该能看到首页', () => {
    cy.visit('/');
    cy.contains('渔鲜直供');
  });

  it('用户应该能够注册', () => {
    cy.visit('/register');
    
    // 匹配 "例: abc123" 的输入框
    cy.get('input[placeholder*="abc123"]').type(username);
    // 匹配昵称
    cy.get('input[placeholder*="测试1"]').type(`用户${randomId}`);
    // 匹配密码
    cy.get('input[placeholder*="密码"]').type(password);
    
    cy.contains('button', '立即注册').click();

    // 修复后，这里应该能成功跳转
    cy.url().should('include', '/login');
  });

  it('用户应该能够登录', () => {
    cy.visit('/login');
    
    cy.get('input[placeholder*="用户名"]').type(username);
    cy.get('input[placeholder*="密码"]').type(password);
    
    cy.contains('button', '登录').click();

    // 验证跳转回首页
    cy.url().should('eq', Cypress.config().baseUrl + '/');
  });
});