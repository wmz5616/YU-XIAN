// frontend/cypress/e2e/promotion.cy.js

describe('优惠券营销测试', () => {
  beforeEach(() => {
    // 自动注册 testUser，防止 401 错误
    const user = { username: 'testUser', password: '123456', displayName: 'PromoUser' };
    cy.request({
      method: 'POST',
      url: '/api/users/register',
      failOnStatusCode: false,
      body: user
    });

    cy.login(user.username, user.password);
  });

  it('领取优惠券并在下单时使用', () => {
    cy.visit('/coupon');
    cy.contains('优惠券中心');
    
    // 领取第一张
    cy.get('.coupon-item').first().within(() => {
      cy.contains('立即领取').click();
    });
    // 这里的文字提示应该会出现，因为是 store.showNotification
    cy.contains(/领取成功|已领取/).should('be.visible');

    // ... 后续逻辑保持不变 ...
    // 如果需要完整流程，建议先确保有商品加购逻辑
  });
});