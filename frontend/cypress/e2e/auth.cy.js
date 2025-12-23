describe("用户认证系统测试", () => {
  const randomId = Math.floor(10000 + Math.random() * 90000);
  const username = `u${randomId}`;
  const password = "password123";

  it("游客应该能看到首页", () => {
    cy.visit("/");
    cy.contains("渔鲜直供");
  });

  it("用户应该能够注册", () => {
    cy.visit("/register");

    cy.get('input[placeholder*="abc123"]').type(username);

    cy.get('input[placeholder*="测试1"]').type(`用户${randomId}`);

    cy.get('input[placeholder*="密码"]').type(password);

    cy.contains("button", "立即注册").click();

    cy.url().should("include", "/login");
  });

  it("用户应该能够登录", () => {
    cy.visit("/login");

    cy.get('input[placeholder*="用户名"]').type(username);
    cy.get('input[placeholder*="密码"]').type(password);

    cy.contains("button", "登录").click();

    cy.url().should("eq", Cypress.config().baseUrl + "/");
  });
});
