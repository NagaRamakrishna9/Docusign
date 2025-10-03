sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast"
], function (Controller, MessageToast) {
    "use strict";

    return Controller.extend("ui5.quickstart.App", {
        onInit() {
            const oApp = this.byId("app");

            // Load stored credentials from localStorage
            const sUser = localStorage.getItem("username") || "";
            const sPass = localStorage.getItem("password") || "";

            // Pre-fill if found
            if (sUser && sPass) {
                this.byId("usernameInput").setValue(sUser);
                this.byId("passwordInput").setValue(sPass);

                // auto-login (optional)
                oApp.to(this.byId("homePage"));
            } else {
                oApp.to(this.byId("loginPage"));
            }
        },

        onLogin() {
            const sUser = this.byId("usernameInput").getValue();
            const sPass = this.byId("passwordInput").getValue();

            if (sUser && sPass) {
                // Save credentials in localStorage
                localStorage.setItem("username", sUser);
                localStorage.setItem("password", sPass);

                MessageToast.show("Login successful!");
                this.byId("app").to(this.byId("homePage"));
            } else {
                sap.m.MessageBox.error("Please enter both username and password");
            }
        },

        onLogout() {
            // clear saved credentials if you want
            // localStorage.removeItem("username");
            // localStorage.removeItem("password");

            this.byId("app").to(this.byId("loginPage"));
        },

        onFileChange: async function (oEvent) {
            var oFileUploader = this.byId("fileUploader");
            var oFile = oEvent.getParameter("files")[0];

            if (!oFile) {
                sap.m.MessageToast.show("Please select a file.");
                return;
            }

            var reader = new FileReader();
            var that = this;

            reader.onload = function (e) {
                var base64Data = e.target.result.split(",")[1];

                var envelopeRequest = {
                    "emailSubject": "Please sign this document",
                    "documents": [
                        {
                            "documentBase64": base64Data,
                            "name": oFile.name,
                            "fileExtension": "pdf",
                            "documentId": "1"
                        }
                    ],
                };

                $.ajax({
                    url: "http://localhost:8080/docusign/envelopes/sender-view", // 👈 full backend URL
                    type: "POST",
                    contentType: "application/json",
                    data: JSON.stringify(envelopeRequest),
                    success: function (response) {
                        sap.m.MessageToast.show("Envelope Created: " + response.envelopeId);
                        if (response.senderUrl) {
                            window.location.href = response.senderUrl;  // 👈 replaces current page
                        }
                    },
                    error: function (xhr, status, error) {
                        sap.m.MessageBox.error("Error: " + xhr.responseText);
                    }
                });
            };

            reader.readAsDataURL(oFile); // Convert file → Base64
        },

       onFileChangeSignature : async function (oEvent) {
            var oFileUploader = this.byId("fileUploader");
            var oFile = oEvent.getParameter("files")[0];

            if (!oFile) {
                sap.m.MessageToast.show("Please select a file.");
                return;
            }

            var reader = new FileReader();
            var that = this;

            reader.onload = function (e) {
                var base64Data = e.target.result.split(",")[1];

                var envelopeRequest = 
                        {
                            "documentBase64": base64Data,
                            "documentName": oFile.name,
                            "recipientName": "pdf",
                            "recipientEmail": "apoorva@inflexiontechfze.com"
                        }

                $.ajax({
                    url: "http://localhost:8080/docusign/envelopes/signature-text",
                    type: "POST",
                    contentType: "application/json",
                    data: JSON.stringify(envelopeRequest),
                    success: function (response) {
                        sap.m.MessageToast.show("Envelope Created: " + response.envelopeId);
                        if (response.senderUrl) {
                            window.location.href = response.senderUrl;
                        }
                    },
                    error: function (xhr, status, error) {
                        sap.m.MessageBox.error("Error: " + xhr.responseText);
                    }
                });
            };

            reader.readAsDataURL(oFile); 
        },

    });
});
