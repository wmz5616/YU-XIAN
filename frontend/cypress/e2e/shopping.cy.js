describe("购物全流程测试", () => {
  const randomId = Math.floor(Math.random() * 10000);
  const testUser = {
    username: `shopUser${randomId}`,
    password: "123456",
    displayName: `Shopper${randomId}`,
  };

  beforeEach(() => {
    cy.request({
      method: "POST",
      url: "/api/users/register",
      failOnStatusCode: false,
      body: testUser,
    });
    cy.login(testUser.username, testUser.password);
  });

  it("完整购物流程：搜索 -> 加购 -> 结算", () => {
    cy.intercept("POST", "/api/orders", {
      statusCode: 200,
      body: { orderId: "ORDER_TEST", message: "下单成功" },
    }).as("createOrder");
    cy.intercept("POST", "/api/orders/*/pay", {
      statusCode: 200,
      body: { success: true },
    }).as("payOrder");
    cy.visit("/");
    cy.get('input[placeholder="搜索"]').type("帝王蟹{enter}");
    cy.wait(1000);
    cy.get(".group").contains("帝王蟹").click();
    cy.contains("button", "立即购买").should("exist").click();
    cy.wait(1000);
    cy.visit("/cart");
    cy.contains("帝王蟹").should("be.visible");
    cy.contains("去结算").click();
    cy.url().should("include", "/checkout");
    cy.contains("添加收货地址").should("be.visible").click();
    cy.get('input[placeholder*="联系人"]')
      .should("be.visible")
      .type("测试机器人");
    cy.get('input[placeholder="手机号码"]').type("13800138000");
    cy.get("textarea").type("自动化测试专用地址");
    cy.contains("button", "保存地址").click();
    cy.contains("测试机器人").should("be.visible");
    cy.contains("立即支付").click();
    cy.contains("支付成功", { timeout: 10000 }).should("be.visible");
  });
});
