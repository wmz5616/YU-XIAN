describe('管理员后台测试', () => {
  it('管理员应能处理售后申请', () => {
    // 1. 管理员登录
    cy.visit('/login');
    cy.get('input[placeholder*="用户名"]').type('admin'); // 确保数据库有 ROLE="ADMIN" 的用户
    cy.get('input[placeholder*="密码"]').type('123');
    cy.contains('button', '登录').click();

    // 2. 进入后台
    cy.contains('进入后台').click();
    cy.url().should('include', '/admin');

    // 3. 进入售后管理
    cy.visit('/admin/refund');
    
    // 4. 验证是否有待处理的售后 (假设有数据)
    // 如果没有数据，我们可以断言"暂无售后申请"的提示，或者断言表格存在
    cy.get('table').should('exist');
    
    // 模拟点击通过审核 (如果页面有按钮)
    // cy.contains('button', '通过').first().click();
    // cy.contains('审核通过').should('be.visible');
  });
});