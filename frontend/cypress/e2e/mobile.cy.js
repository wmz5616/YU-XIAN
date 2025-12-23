describe("移动端响应式测试", () => {
  beforeEach(() => {
    cy.viewport("iphone-x");
    cy.visit("/");
  });

  it("移动端应显示底部导航栏", () => {
    cy.get("nav.fixed.bottom-0").should("exist");

    cy.get("nav.fixed.bottom-0").contains("购物车").click({ force: true });
    cy.url().should("include", "/cart");
  });
});
