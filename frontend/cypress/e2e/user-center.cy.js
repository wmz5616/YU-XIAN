describe("用户中心与售后流程测试", () => {
  const randomId = Math.floor(Math.random() * 10000);
  const user = {
    username: `user${randomId}`,
    password: "123456",
    displayName: `User${randomId}`,
  };

  beforeEach(() => {
    cy.request({
      method: "POST",
      url: "/api/users/register",
      failOnStatusCode: false,
      body: user,
    });
    cy.login(user.username, user.password);
  });

  it("应该能查看个人积分和资料", () => {
    cy.visit("/profile");
    cy.contains("空间").should("be.visible");
    cy.contains("积分").should("exist");
    cy.contains(user.username).should("exist");
  });

  it("新用户应显示暂无订单", () => {
    cy.visit("/orders");
    cy.contains("我的订单");
    cy.contains("暂无订单").should("be.visible");
    cy.contains("📦").should("exist");
    cy.contains("去逛逛").should("exist");
  });
});
