
//import {UserManager} from "./scripts/oidc-client-ts/oidc-client-ts.js"
// import {axios} from "./scripts/axios/axios.min.js"


const idp_base_url = "http://localhost:8080/uaa"
rabbit_port = window.location.port ? ":" +  window.location.port : ""
rabbit_base_uri = window.location.protocol + "//" + window.location.hostname + rabbit_port
rabbit_redirect_uri = rabbit_base_uri
rabbit_login_uri = rabbit_base_uri + "/login.html"

const settings = {
    //userStore: new WebStorageStateStore({ store: window.localStorage }),
    authority: idp_base_url,
    client_id: "rabbit_client_code",
    client_secret: "rabbit_client_code",
    redirect_uri: rabbit_redirect_uri + "/login-callback.html",
    post_logout_redirect_uri: rabbit_redirect_uri * "/logout-callback.html",
    response_type: "code",
    scope: "openid profile rabbitmq.*",
    resource: "rabbitmq",
    //scope: 'openid profile api offline_access',

    //automaticSilentRenew: false,
    //validateSubOnSilentRenew: true,
    //silentRequestTimeout: 10000,

    //loadUserInfo: true,
    //monitorAnonymousSession: true,
    filterProtocolClaims: true,
    revokeAccessTokenOnSignout: true,

};

oidc.Log.setLogger(console);
oidc.Log.setLevel(oidc.Log.INFO);

function log() {
    message = ""
    Array.prototype.forEach.call(arguments, function(msg) {
        if (msg instanceof Error) {
            msg = "Error: " + msg.message;
        }
        else if (typeof msg !== "string") {
            msg = JSON.stringify(msg, null, 2);
        }
        message += msg
    });
    console.log(message)
}


var mgr = new oidc.UserManager(settings);


function registerCallbacks() {
  mgr.events.addUserLoaded(function (user) {
      console.log("addUserLoaded=> ", user);
      mgr.getUser().then(function() {
          console.log("getUser loaded user after userLoaded event fired");
      });
  });
  mgr.events.addUserUnloaded(function (e) {
      console.log("addUserUnloaded=> ", e);
  });

  mgr.events.addUserSignedIn(function (e) {
      log("addUserSignedIn=> " , e);
  });
  mgr.events.addUserSignedOut(function (e) {
      log("addUserSignedOut=> ", e);
  });

}
function isLoggedIn() {
    return mgr.getUser().then(user => {
        if (!user) {
            return { "loggedIn": false };
        }
        return { "user": user, "loggedIn": !user.expired };
    });
}


function initiateLogin() {
    mgr.signinRedirect({ state: { foo: "bar" } /*, useReplaceToNavigate: true*/ }).then(function() {
        log("signinRedirect done");
    }).catch(function(err) {
        console.error(err);
        log(err);
    });
}
function redirectToHome() {
  location.href = "/index.html"
}
function redirectToLogin() {
  location.href = "/login.html"
}
function completeLogin() {
    mgr.signinRedirectCallback().then(user => redirectToHome()).catch(function(err) {
        console.error(err);
        log(err);
    });
}

function initiateLogout() {
    mgr.signoutRedirect();
}
function completeLogout() {
    mgr.signoutRedirectCallback().then(_ => redirectToLogin());
}
