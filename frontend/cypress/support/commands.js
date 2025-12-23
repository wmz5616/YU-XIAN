Cypress.Commands.add("login", (username, password) => {
  cy.session([username, password], () => {
    cy.request({
      method: "POST",
      url: "/api/users/login",
      body: { username, password },
    }).then(({ body }) => {
      window.localStorage.setItem("yuxian_token", body.token);
      window.localStorage.setItem("role", body.role);

      if (body.user) {
        window.localStorage.setItem("yuxian_user", JSON.stringify(body.user));
      }
    });
  });

  cy.visit("/");
});
