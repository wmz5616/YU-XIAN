// frontend/cypress/e2e/user-center.cy.js

describe('用户中心与售后流程测试', () => {
  const user = { username: 'testUser', password: '123456', displayName: 'UserCenter' };

  beforeEach(() => {
    // 自动注册
    cy.request({
      method: 'POST',
      url: '/api/users/register',
      failOnStatusCode: false,
      body: user
    });
    
    cy.login(user.username, user.password);
  });

  it('应该能查看个人积分和资料', () => {
    cy.visit('/profile');
    cy.contains('个人中心');
    cy.contains('积分余额').should('exist');
    cy.contains(user.username);
  });

  // 注意：这个测试依赖“历史订单”，如果是新注册的用户，这里必然为空
  // 所以这个测试在“空号”情况下会失败是正常的。
  // 建议改为测试“空订单状态”
  it('新用户应显示暂无订单', () => {
    cy.visit('/orders');
    cy.contains('我的订单');
    // 验证空状态图片或文字
    cy.get('img[src*="empty"]').should('exist'); 
  });
});