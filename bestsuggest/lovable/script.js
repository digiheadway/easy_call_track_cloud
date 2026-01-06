var jc = e => {
    throw TypeError(e)
}
;
var vl = (e, t, n) => t.has(e) || jc("Cannot " + n);
var T = (e, t, n) => (vl(e, t, "read from private field"),
n ? n.call(e) : t.get(e))
  , q = (e, t, n) => t.has(e) ? jc("Cannot add the same private member more than once") : t instanceof WeakSet ? t.add(e) : t.set(e, n)
  , V = (e, t, n, r) => (vl(e, t, "write to private field"),
r ? r.call(e, n) : t.set(e, n),
n)
  , Pe = (e, t, n) => (vl(e, t, "access private method"),
n);
var yi = (e, t, n, r) => ({
    set _(o) {
        V(e, t, o, n)
    },
    get _() {
        return T(e, t, r)
    }
});
function lg(e, t) {
    for (var n = 0; n < t.length; n++) {
        const r = t[n];
        if (typeof r != "string" && !Array.isArray(r)) {
            for (const o in r)
                if (o !== "default" && !(o in e)) {
                    const i = Object.getOwnPropertyDescriptor(r, o);
                    i && Object.defineProperty(e, o, i.get ? i : {
                        enumerable: !0,
                        get: () => r[o]
                    })
                }
        }
    }
    return Object.freeze(Object.defineProperty(e, Symbol.toStringTag, {
        value: "Module"
    }))
}
(function() {
    const t = document.createElement("link").relList;
    if (t && t.supports && t.supports("modulepreload"))
        return;
    for (const o of document.querySelectorAll('link[rel="modulepreload"]'))
        r(o);
    new MutationObserver(o => {
        for (const i of o)
            if (i.type === "childList")
                for (const s of i.addedNodes)
                    s.tagName === "LINK" && s.rel === "modulepreload" && r(s)
    }
    ).observe(document, {
        childList: !0,
        subtree: !0
    });
    function n(o) {
        const i = {};
        return o.integrity && (i.integrity = o.integrity),
        o.referrerPolicy && (i.referrerPolicy = o.referrerPolicy),
        o.crossOrigin === "use-credentials" ? i.credentials = "include" : o.crossOrigin === "anonymous" ? i.credentials = "omit" : i.credentials = "same-origin",
        i
    }
    function r(o) {
        if (o.ep)
            return;
        o.ep = !0;
        const i = n(o);
        fetch(o.href, i)
    }
}
)();
function Of(e) {
    return e && e.__esModule && Object.prototype.hasOwnProperty.call(e, "default") ? e.default : e
}
var _f = {
    exports: {}
}
  , Ms = {}
  , Lf = {
    exports: {}
}
  , G = {};
/**
 * @license React
 * react.production.min.js
 *
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
var ii = Symbol.for("react.element")
  , ag = Symbol.for("react.portal")
  , ug = Symbol.for("react.fragment")
  , cg = Symbol.for("react.strict_mode")
  , dg = Symbol.for("react.profiler")
  , fg = Symbol.for("react.provider")
  , pg = Symbol.for("react.context")
  , hg = Symbol.for("react.forward_ref")
  , mg = Symbol.for("react.suspense")
  , vg = Symbol.for("react.memo")
  , gg = Symbol.for("react.lazy")
  , Oc = Symbol.iterator;
function yg(e) {
    return e === null || typeof e != "object" ? null : (e = Oc && e[Oc] || e["@@iterator"],
    typeof e == "function" ? e : null)
}
var Mf = {
    isMounted: function() {
        return !1
    },
    enqueueForceUpdate: function() {},
    enqueueReplaceState: function() {},
    enqueueSetState: function() {}
}
  , If = Object.assign
  , Df = {};
function Xr(e, t, n) {
    this.props = e,
    this.context = t,
    this.refs = Df,
    this.updater = n || Mf
}
Xr.prototype.isReactComponent = {};
Xr.prototype.setState = function(e, t) {
    if (typeof e != "object" && typeof e != "function" && e != null)
        throw Error("setState(...): takes an object of state variables to update or a function which returns an object of state variables.");
    this.updater.enqueueSetState(this, e, t, "setState")
}
;
Xr.prototype.forceUpdate = function(e) {
    this.updater.enqueueForceUpdate(this, e, "forceUpdate")
}
;
function zf() {}
zf.prototype = Xr.prototype;
function lu(e, t, n) {
    this.props = e,
    this.context = t,
    this.refs = Df,
    this.updater = n || Mf
}
var au = lu.prototype = new zf;
au.constructor = lu;
If(au, Xr.prototype);
au.isPureReactComponent = !0;
var _c = Array.isArray
  , Ff = Object.prototype.hasOwnProperty
  , uu = {
    current: null
}
  , $f = {
    key: !0,
    ref: !0,
    __self: !0,
    __source: !0
};
function Uf(e, t, n) {
    var r, o = {}, i = null, s = null;
    if (t != null)
        for (r in t.ref !== void 0 && (s = t.ref),
        t.key !== void 0 && (i = "" + t.key),
        t)
            Ff.call(t, r) && !$f.hasOwnProperty(r) && (o[r] = t[r]);
    var l = arguments.length - 2;
    if (l === 1)
        o.children = n;
    else if (1 < l) {
        for (var a = Array(l), u = 0; u < l; u++)
            a[u] = arguments[u + 2];
        o.children = a
    }
    if (e && e.defaultProps)
        for (r in l = e.defaultProps,
        l)
            o[r] === void 0 && (o[r] = l[r]);
    return {
        $$typeof: ii,
        type: e,
        key: i,
        ref: s,
        props: o,
        _owner: uu.current
    }
}
function xg(e, t) {
    return {
        $$typeof: ii,
        type: e.type,
        key: t,
        ref: e.ref,
        props: e.props,
        _owner: e._owner
    }
}
function cu(e) {
    return typeof e == "object" && e !== null && e.$$typeof === ii
}
function wg(e) {
    var t = {
        "=": "=0",
        ":": "=2"
    };
    return "$" + e.replace(/[=:]/g, function(n) {
        return t[n]
    })
}
var Lc = /\/+/g;
function gl(e, t) {
    return typeof e == "object" && e !== null && e.key != null ? wg("" + e.key) : t.toString(36)
}
function Bi(e, t, n, r, o) {
    var i = typeof e;
    (i === "undefined" || i === "boolean") && (e = null);
    var s = !1;
    if (e === null)
        s = !0;
    else
        switch (i) {
        case "string":
        case "number":
            s = !0;
            break;
        case "object":
            switch (e.$$typeof) {
            case ii:
            case ag:
                s = !0
            }
        }
    if (s)
        return s = e,
        o = o(s),
        e = r === "" ? "." + gl(s, 0) : r,
        _c(o) ? (n = "",
        e != null && (n = e.replace(Lc, "$&/") + "/"),
        Bi(o, t, n, "", function(u) {
            return u
        })) : o != null && (cu(o) && (o = xg(o, n + (!o.key || s && s.key === o.key ? "" : ("" + o.key).replace(Lc, "$&/") + "/") + e)),
        t.push(o)),
        1;
    if (s = 0,
    r = r === "" ? "." : r + ":",
    _c(e))
        for (var l = 0; l < e.length; l++) {
            i = e[l];
            var a = r + gl(i, l);
            s += Bi(i, t, n, a, o)
        }
    else if (a = yg(e),
    typeof a == "function")
        for (e = a.call(e),
        l = 0; !(i = e.next()).done; )
            i = i.value,
            a = r + gl(i, l++),
            s += Bi(i, t, n, a, o);
    else if (i === "object")
        throw t = String(e),
        Error("Objects are not valid as a React child (found: " + (t === "[object Object]" ? "object with keys {" + Object.keys(e).join(", ") + "}" : t) + "). If you meant to render a collection of children, use an array instead.");
    return s
}
function xi(e, t, n) {
    if (e == null)
        return e;
    var r = []
      , o = 0;
    return Bi(e, r, "", "", function(i) {
        return t.call(n, i, o++)
    }),
    r
}
function Eg(e) {
    if (e._status === -1) {
        var t = e._result;
        t = t(),
        t.then(function(n) {
            (e._status === 0 || e._status === -1) && (e._status = 1,
            e._result = n)
        }, function(n) {
            (e._status === 0 || e._status === -1) && (e._status = 2,
            e._result = n)
        }),
        e._status === -1 && (e._status = 0,
        e._result = t)
    }
    if (e._status === 1)
        return e._result.default;
    throw e._result
}
var De = {
    current: null
}
  , Wi = {
    transition: null
}
  , Sg = {
    ReactCurrentDispatcher: De,
    ReactCurrentBatchConfig: Wi,
    ReactCurrentOwner: uu
};
function Bf() {
    throw Error("act(...) is not supported in production builds of React.")
}
G.Children = {
    map: xi,
    forEach: function(e, t, n) {
        xi(e, function() {
            t.apply(this, arguments)
        }, n)
    },
    count: function(e) {
        var t = 0;
        return xi(e, function() {
            t++
        }),
        t
    },
    toArray: function(e) {
        return xi(e, function(t) {
            return t
        }) || []
    },
    only: function(e) {
        if (!cu(e))
            throw Error("React.Children.only expected to receive a single React element child.");
        return e
    }
};
G.Component = Xr;
G.Fragment = ug;
G.Profiler = dg;
G.PureComponent = lu;
G.StrictMode = cg;
G.Suspense = mg;
G.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED = Sg;
G.act = Bf;
G.cloneElement = function(e, t, n) {
    if (e == null)
        throw Error("React.cloneElement(...): The argument must be a React element, but you passed " + e + ".");
    var r = If({}, e.props)
      , o = e.key
      , i = e.ref
      , s = e._owner;
    if (t != null) {
        if (t.ref !== void 0 && (i = t.ref,
        s = uu.current),
        t.key !== void 0 && (o = "" + t.key),
        e.type && e.type.defaultProps)
            var l = e.type.defaultProps;
        for (a in t)
            Ff.call(t, a) && !$f.hasOwnProperty(a) && (r[a] = t[a] === void 0 && l !== void 0 ? l[a] : t[a])
    }
    var a = arguments.length - 2;
    if (a === 1)
        r.children = n;
    else if (1 < a) {
        l = Array(a);
        for (var u = 0; u < a; u++)
            l[u] = arguments[u + 2];
        r.children = l
    }
    return {
        $$typeof: ii,
        type: e.type,
        key: o,
        ref: i,
        props: r,
        _owner: s
    }
}
;
G.createContext = function(e) {
    return e = {
        $$typeof: pg,
        _currentValue: e,
        _currentValue2: e,
        _threadCount: 0,
        Provider: null,
        Consumer: null,
        _defaultValue: null,
        _globalName: null
    },
    e.Provider = {
        $$typeof: fg,
        _context: e
    },
    e.Consumer = e
}
;
G.createElement = Uf;
G.createFactory = function(e) {
    var t = Uf.bind(null, e);
    return t.type = e,
    t
}
;
G.createRef = function() {
    return {
        current: null
    }
}
;
G.forwardRef = function(e) {
    return {
        $$typeof: hg,
        render: e
    }
}
;
G.isValidElement = cu;
G.lazy = function(e) {
    return {
        $$typeof: gg,
        _payload: {
            _status: -1,
            _result: e
        },
        _init: Eg
    }
}
;
G.memo = function(e, t) {
    return {
        $$typeof: vg,
        type: e,
        compare: t === void 0 ? null : t
    }
}
;
G.startTransition = function(e) {
    var t = Wi.transition;
    Wi.transition = {};
    try {
        e()
    } finally {
        Wi.transition = t
    }
}
;
G.unstable_act = Bf;
G.useCallback = function(e, t) {
    return De.current.useCallback(e, t)
}
;
G.useContext = function(e) {
    return De.current.useContext(e)
}
;
G.useDebugValue = function() {}
;
G.useDeferredValue = function(e) {
    return De.current.useDeferredValue(e)
}
;
G.useEffect = function(e, t) {
    return De.current.useEffect(e, t)
}
;
G.useId = function() {
    return De.current.useId()
}
;
G.useImperativeHandle = function(e, t, n) {
    return De.current.useImperativeHandle(e, t, n)
}
;
G.useInsertionEffect = function(e, t) {
    return De.current.useInsertionEffect(e, t)
}
;
G.useLayoutEffect = function(e, t) {
    return De.current.useLayoutEffect(e, t)
}
;
G.useMemo = function(e, t) {
    return De.current.useMemo(e, t)
}
;
G.useReducer = function(e, t, n) {
    return De.current.useReducer(e, t, n)
}
;
G.useRef = function(e) {
    return De.current.useRef(e)
}
;
G.useState = function(e) {
    return De.current.useState(e)
}
;
G.useSyncExternalStore = function(e, t, n) {
    return De.current.useSyncExternalStore(e, t, n)
}
;
G.useTransition = function() {
    return De.current.useTransition()
}
;
G.version = "18.3.1";
Lf.exports = G;
var g = Lf.exports;
const R = Of(g)
  , Wf = lg({
    __proto__: null,
    default: R
}, [g]);
/**
 * @license React
 * react-jsx-runtime.production.min.js
 *
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
var Cg = g
  , bg = Symbol.for("react.element")
  , kg = Symbol.for("react.fragment")
  , Pg = Object.prototype.hasOwnProperty
  , Ng = Cg.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED.ReactCurrentOwner
  , Tg = {
    key: !0,
    ref: !0,
    __self: !0,
    __source: !0
};
function Vf(e, t, n) {
    var r, o = {}, i = null, s = null;
    n !== void 0 && (i = "" + n),
    t.key !== void 0 && (i = "" + t.key),
    t.ref !== void 0 && (s = t.ref);
    for (r in t)
        Pg.call(t, r) && !Tg.hasOwnProperty(r) && (o[r] = t[r]);
    if (e && e.defaultProps)
        for (r in t = e.defaultProps,
        t)
            o[r] === void 0 && (o[r] = t[r]);
    return {
        $$typeof: bg,
        type: e,
        key: i,
        ref: s,
        props: o,
        _owner: Ng.current
    }
}
Ms.Fragment = kg;
Ms.jsx = Vf;
Ms.jsxs = Vf;
_f.exports = Ms;
var m = _f.exports
  , Hf = {
    exports: {}
}
  , Je = {}
  , Qf = {
    exports: {}
}
  , Kf = {};
/**
 * @license React
 * scheduler.production.min.js
 *
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
(function(e) {
    function t(k, j) {
        var z = k.length;
        k.push(j);
        e: for (; 0 < z; ) {
            var I = z - 1 >>> 1
              , F = k[I];
            if (0 < o(F, j))
                k[I] = j,
                k[z] = F,
                z = I;
            else
                break e
        }
    }
    function n(k) {
        return k.length === 0 ? null : k[0]
    }
    function r(k) {
        if (k.length === 0)
            return null;
        var j = k[0]
          , z = k.pop();
        if (z !== j) {
            k[0] = z;
            e: for (var I = 0, F = k.length, Y = F >>> 1; I < Y; ) {
                var ae = 2 * (I + 1) - 1
                  , Ve = k[ae]
                  , Z = ae + 1
                  , ut = k[Z];
                if (0 > o(Ve, z))
                    Z < F && 0 > o(ut, Ve) ? (k[I] = ut,
                    k[Z] = z,
                    I = Z) : (k[I] = Ve,
                    k[ae] = z,
                    I = ae);
                else if (Z < F && 0 > o(ut, z))
                    k[I] = ut,
                    k[Z] = z,
                    I = Z;
                else
                    break e
            }
        }
        return j
    }
    function o(k, j) {
        var z = k.sortIndex - j.sortIndex;
        return z !== 0 ? z : k.id - j.id
    }
    if (typeof performance == "object" && typeof performance.now == "function") {
        var i = performance;
        e.unstable_now = function() {
            return i.now()
        }
    } else {
        var s = Date
          , l = s.now();
        e.unstable_now = function() {
            return s.now() - l
        }
    }
    var a = []
      , u = []
      , d = 1
      , f = null
      , c = 3
      , y = !1
      , w = !1
      , x = !1
      , E = typeof setTimeout == "function" ? setTimeout : null
      , h = typeof clearTimeout == "function" ? clearTimeout : null
      , p = typeof setImmediate < "u" ? setImmediate : null;
    typeof navigator < "u" && navigator.scheduling !== void 0 && navigator.scheduling.isInputPending !== void 0 && navigator.scheduling.isInputPending.bind(navigator.scheduling);
    function v(k) {
        for (var j = n(u); j !== null; ) {
            if (j.callback === null)
                r(u);
            else if (j.startTime <= k)
                r(u),
                j.sortIndex = j.expirationTime,
                t(a, j);
            else
                break;
            j = n(u)
        }
    }
    function S(k) {
        if (x = !1,
        v(k),
        !w)
            if (n(a) !== null)
                w = !0,
                U(C);
            else {
                var j = n(u);
                j !== null && K(S, j.startTime - k)
            }
    }
    function C(k, j) {
        w = !1,
        x && (x = !1,
        h(N),
        N = -1),
        y = !0;
        var z = c;
        try {
            for (v(j),
            f = n(a); f !== null && (!(f.expirationTime > j) || k && !$()); ) {
                var I = f.callback;
                if (typeof I == "function") {
                    f.callback = null,
                    c = f.priorityLevel;
                    var F = I(f.expirationTime <= j);
                    j = e.unstable_now(),
                    typeof F == "function" ? f.callback = F : f === n(a) && r(a),
                    v(j)
                } else
                    r(a);
                f = n(a)
            }
            if (f !== null)
                var Y = !0;
            else {
                var ae = n(u);
                ae !== null && K(S, ae.startTime - j),
                Y = !1
            }
            return Y
        } finally {
            f = null,
            c = z,
            y = !1
        }
    }
    var P = !1
      , b = null
      , N = -1
      , _ = 5
      , O = -1;
    function $() {
        return !(e.unstable_now() - O < _)
    }
    function D() {
        if (b !== null) {
            var k = e.unstable_now();
            O = k;
            var j = !0;
            try {
                j = b(!0, k)
            } finally {
                j ? H() : (P = !1,
                b = null)
            }
        } else
            P = !1
    }
    var H;
    if (typeof p == "function")
        H = function() {
            p(D)
        }
        ;
    else if (typeof MessageChannel < "u") {
        var L = new MessageChannel
          , Q = L.port2;
        L.port1.onmessage = D,
        H = function() {
            Q.postMessage(null)
        }
    } else
        H = function() {
            E(D, 0)
        }
        ;
    function U(k) {
        b = k,
        P || (P = !0,
        H())
    }
    function K(k, j) {
        N = E(function() {
            k(e.unstable_now())
        }, j)
    }
    e.unstable_IdlePriority = 5,
    e.unstable_ImmediatePriority = 1,
    e.unstable_LowPriority = 4,
    e.unstable_NormalPriority = 3,
    e.unstable_Profiling = null,
    e.unstable_UserBlockingPriority = 2,
    e.unstable_cancelCallback = function(k) {
        k.callback = null
    }
    ,
    e.unstable_continueExecution = function() {
        w || y || (w = !0,
        U(C))
    }
    ,
    e.unstable_forceFrameRate = function(k) {
        0 > k || 125 < k ? console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported") : _ = 0 < k ? Math.floor(1e3 / k) : 5
    }
    ,
    e.unstable_getCurrentPriorityLevel = function() {
        return c
    }
    ,
    e.unstable_getFirstCallbackNode = function() {
        return n(a)
    }
    ,
    e.unstable_next = function(k) {
        switch (c) {
        case 1:
        case 2:
        case 3:
            var j = 3;
            break;
        default:
            j = c
        }
        var z = c;
        c = j;
        try {
            return k()
        } finally {
            c = z
        }
    }
    ,
    e.unstable_pauseExecution = function() {}
    ,
    e.unstable_requestPaint = function() {}
    ,
    e.unstable_runWithPriority = function(k, j) {
        switch (k) {
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
            break;
        default:
            k = 3
        }
        var z = c;
        c = k;
        try {
            return j()
        } finally {
            c = z
        }
    }
    ,
    e.unstable_scheduleCallback = function(k, j, z) {
        var I = e.unstable_now();
        switch (typeof z == "object" && z !== null ? (z = z.delay,
        z = typeof z == "number" && 0 < z ? I + z : I) : z = I,
        k) {
        case 1:
            var F = -1;
            break;
        case 2:
            F = 250;
            break;
        case 5:
            F = 1073741823;
            break;
        case 4:
            F = 1e4;
            break;
        default:
            F = 5e3
        }
        return F = z + F,
        k = {
            id: d++,
            callback: j,
            priorityLevel: k,
            startTime: z,
            expirationTime: F,
            sortIndex: -1
        },
        z > I ? (k.sortIndex = z,
        t(u, k),
        n(a) === null && k === n(u) && (x ? (h(N),
        N = -1) : x = !0,
        K(S, z - I))) : (k.sortIndex = F,
        t(a, k),
        w || y || (w = !0,
        U(C))),
        k
    }
    ,
    e.unstable_shouldYield = $,
    e.unstable_wrapCallback = function(k) {
        var j = c;
        return function() {
            var z = c;
            c = j;
            try {
                return k.apply(this, arguments)
            } finally {
                c = z
            }
        }
    }
}
)(Kf);
Qf.exports = Kf;
var Rg = Qf.exports;
/**
 * @license React
 * react-dom.production.min.js
 *
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
var Ag = g
  , Ze = Rg;
function A(e) {
    for (var t = "https://reactjs.org/docs/error-decoder.html?invariant=" + e, n = 1; n < arguments.length; n++)
        t += "&args[]=" + encodeURIComponent(arguments[n]);
    return "Minified React error #" + e + "; visit " + t + " for the full message or use the non-minified dev environment for full errors and additional helpful warnings."
}
var Gf = new Set
  , _o = {};
function rr(e, t) {
    Ur(e, t),
    Ur(e + "Capture", t)
}
function Ur(e, t) {
    for (_o[e] = t,
    e = 0; e < t.length; e++)
        Gf.add(t[e])
}
var Wt = !(typeof window > "u" || typeof window.document > "u" || typeof window.document.createElement > "u")
  , ql = Object.prototype.hasOwnProperty
  , jg = /^[:A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD][:A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD\-.0-9\u00B7\u0300-\u036F\u203F-\u2040]*$/
  , Mc = {}
  , Ic = {};
function Og(e) {
    return ql.call(Ic, e) ? !0 : ql.call(Mc, e) ? !1 : jg.test(e) ? Ic[e] = !0 : (Mc[e] = !0,
    !1)
}
function _g(e, t, n, r) {
    if (n !== null && n.type === 0)
        return !1;
    switch (typeof t) {
    case "function":
    case "symbol":
        return !0;
    case "boolean":
        return r ? !1 : n !== null ? !n.acceptsBooleans : (e = e.toLowerCase().slice(0, 5),
        e !== "data-" && e !== "aria-");
    default:
        return !1
    }
}
function Lg(e, t, n, r) {
    if (t === null || typeof t > "u" || _g(e, t, n, r))
        return !0;
    if (r)
        return !1;
    if (n !== null)
        switch (n.type) {
        case 3:
            return !t;
        case 4:
            return t === !1;
        case 5:
            return isNaN(t);
        case 6:
            return isNaN(t) || 1 > t
        }
    return !1
}
function ze(e, t, n, r, o, i, s) {
    this.acceptsBooleans = t === 2 || t === 3 || t === 4,
    this.attributeName = r,
    this.attributeNamespace = o,
    this.mustUseProperty = n,
    this.propertyName = e,
    this.type = t,
    this.sanitizeURL = i,
    this.removeEmptyString = s
}
var ke = {};
"children dangerouslySetInnerHTML defaultValue defaultChecked innerHTML suppressContentEditableWarning suppressHydrationWarning style".split(" ").forEach(function(e) {
    ke[e] = new ze(e,0,!1,e,null,!1,!1)
});
[["acceptCharset", "accept-charset"], ["className", "class"], ["htmlFor", "for"], ["httpEquiv", "http-equiv"]].forEach(function(e) {
    var t = e[0];
    ke[t] = new ze(t,1,!1,e[1],null,!1,!1)
});
["contentEditable", "draggable", "spellCheck", "value"].forEach(function(e) {
    ke[e] = new ze(e,2,!1,e.toLowerCase(),null,!1,!1)
});
["autoReverse", "externalResourcesRequired", "focusable", "preserveAlpha"].forEach(function(e) {
    ke[e] = new ze(e,2,!1,e,null,!1,!1)
});
"allowFullScreen async autoFocus autoPlay controls default defer disabled disablePictureInPicture disableRemotePlayback formNoValidate hidden loop noModule noValidate open playsInline readOnly required reversed scoped seamless itemScope".split(" ").forEach(function(e) {
    ke[e] = new ze(e,3,!1,e.toLowerCase(),null,!1,!1)
});
["checked", "multiple", "muted", "selected"].forEach(function(e) {
    ke[e] = new ze(e,3,!0,e,null,!1,!1)
});
["capture", "download"].forEach(function(e) {
    ke[e] = new ze(e,4,!1,e,null,!1,!1)
});
["cols", "rows", "size", "span"].forEach(function(e) {
    ke[e] = new ze(e,6,!1,e,null,!1,!1)
});
["rowSpan", "start"].forEach(function(e) {
    ke[e] = new ze(e,5,!1,e.toLowerCase(),null,!1,!1)
});
var du = /[\-:]([a-z])/g;
function fu(e) {
    return e[1].toUpperCase()
}
"accent-height alignment-baseline arabic-form baseline-shift cap-height clip-path clip-rule color-interpolation color-interpolation-filters color-profile color-rendering dominant-baseline enable-background fill-opacity fill-rule flood-color flood-opacity font-family font-size font-size-adjust font-stretch font-style font-variant font-weight glyph-name glyph-orientation-horizontal glyph-orientation-vertical horiz-adv-x horiz-origin-x image-rendering letter-spacing lighting-color marker-end marker-mid marker-start overline-position overline-thickness paint-order panose-1 pointer-events rendering-intent shape-rendering stop-color stop-opacity strikethrough-position strikethrough-thickness stroke-dasharray stroke-dashoffset stroke-linecap stroke-linejoin stroke-miterlimit stroke-opacity stroke-width text-anchor text-decoration text-rendering underline-position underline-thickness unicode-bidi unicode-range units-per-em v-alphabetic v-hanging v-ideographic v-mathematical vector-effect vert-adv-y vert-origin-x vert-origin-y word-spacing writing-mode xmlns:xlink x-height".split(" ").forEach(function(e) {
    var t = e.replace(du, fu);
    ke[t] = new ze(t,1,!1,e,null,!1,!1)
});
"xlink:actuate xlink:arcrole xlink:role xlink:show xlink:title xlink:type".split(" ").forEach(function(e) {
    var t = e.replace(du, fu);
    ke[t] = new ze(t,1,!1,e,"http://www.w3.org/1999/xlink",!1,!1)
});
["xml:base", "xml:lang", "xml:space"].forEach(function(e) {
    var t = e.replace(du, fu);
    ke[t] = new ze(t,1,!1,e,"http://www.w3.org/XML/1998/namespace",!1,!1)
});
["tabIndex", "crossOrigin"].forEach(function(e) {
    ke[e] = new ze(e,1,!1,e.toLowerCase(),null,!1,!1)
});
ke.xlinkHref = new ze("xlinkHref",1,!1,"xlink:href","http://www.w3.org/1999/xlink",!0,!1);
["src", "href", "action", "formAction"].forEach(function(e) {
    ke[e] = new ze(e,1,!1,e.toLowerCase(),null,!0,!0)
});
function pu(e, t, n, r) {
    var o = ke.hasOwnProperty(t) ? ke[t] : null;
    (o !== null ? o.type !== 0 : r || !(2 < t.length) || t[0] !== "o" && t[0] !== "O" || t[1] !== "n" && t[1] !== "N") && (Lg(t, n, o, r) && (n = null),
    r || o === null ? Og(t) && (n === null ? e.removeAttribute(t) : e.setAttribute(t, "" + n)) : o.mustUseProperty ? e[o.propertyName] = n === null ? o.type === 3 ? !1 : "" : n : (t = o.attributeName,
    r = o.attributeNamespace,
    n === null ? e.removeAttribute(t) : (o = o.type,
    n = o === 3 || o === 4 && n === !0 ? "" : "" + n,
    r ? e.setAttributeNS(r, t, n) : e.setAttribute(t, n))))
}
var Yt = Ag.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED
  , wi = Symbol.for("react.element")
  , fr = Symbol.for("react.portal")
  , pr = Symbol.for("react.fragment")
  , hu = Symbol.for("react.strict_mode")
  , Zl = Symbol.for("react.profiler")
  , Yf = Symbol.for("react.provider")
  , Xf = Symbol.for("react.context")
  , mu = Symbol.for("react.forward_ref")
  , Jl = Symbol.for("react.suspense")
  , ea = Symbol.for("react.suspense_list")
  , vu = Symbol.for("react.memo")
  , ln = Symbol.for("react.lazy")
  , qf = Symbol.for("react.offscreen")
  , Dc = Symbol.iterator;
function lo(e) {
    return e === null || typeof e != "object" ? null : (e = Dc && e[Dc] || e["@@iterator"],
    typeof e == "function" ? e : null)
}
var de = Object.assign, yl;
function xo(e) {
    if (yl === void 0)
        try {
            throw Error()
        } catch (n) {
            var t = n.stack.trim().match(/\n( *(at )?)/);
            yl = t && t[1] || ""
        }
    return `
` + yl + e
}
var xl = !1;
function wl(e, t) {
    if (!e || xl)
        return "";
    xl = !0;
    var n = Error.prepareStackTrace;
    Error.prepareStackTrace = void 0;
    try {
        if (t)
            if (t = function() {
                throw Error()
            }
            ,
            Object.defineProperty(t.prototype, "props", {
                set: function() {
                    throw Error()
                }
            }),
            typeof Reflect == "object" && Reflect.construct) {
                try {
                    Reflect.construct(t, [])
                } catch (u) {
                    var r = u
                }
                Reflect.construct(e, [], t)
            } else {
                try {
                    t.call()
                } catch (u) {
                    r = u
                }
                e.call(t.prototype)
            }
        else {
            try {
                throw Error()
            } catch (u) {
                r = u
            }
            e()
        }
    } catch (u) {
        if (u && r && typeof u.stack == "string") {
            for (var o = u.stack.split(`
`), i = r.stack.split(`
`), s = o.length - 1, l = i.length - 1; 1 <= s && 0 <= l && o[s] !== i[l]; )
                l--;
            for (; 1 <= s && 0 <= l; s--,
            l--)
                if (o[s] !== i[l]) {
                    if (s !== 1 || l !== 1)
                        do
                            if (s--,
                            l--,
                            0 > l || o[s] !== i[l]) {
                                var a = `
` + o[s].replace(" at new ", " at ");
                                return e.displayName && a.includes("<anonymous>") && (a = a.replace("<anonymous>", e.displayName)),
                                a
                            }
                        while (1 <= s && 0 <= l);
                    break
                }
        }
    } finally {
        xl = !1,
        Error.prepareStackTrace = n
    }
    return (e = e ? e.displayName || e.name : "") ? xo(e) : ""
}
function Mg(e) {
    switch (e.tag) {
    case 5:
        return xo(e.type);
    case 16:
        return xo("Lazy");
    case 13:
        return xo("Suspense");
    case 19:
        return xo("SuspenseList");
    case 0:
    case 2:
    case 15:
        return e = wl(e.type, !1),
        e;
    case 11:
        return e = wl(e.type.render, !1),
        e;
    case 1:
        return e = wl(e.type, !0),
        e;
    default:
        return ""
    }
}
function ta(e) {
    if (e == null)
        return null;
    if (typeof e == "function")
        return e.displayName || e.name || null;
    if (typeof e == "string")
        return e;
    switch (e) {
    case pr:
        return "Fragment";
    case fr:
        return "Portal";
    case Zl:
        return "Profiler";
    case hu:
        return "StrictMode";
    case Jl:
        return "Suspense";
    case ea:
        return "SuspenseList"
    }
    if (typeof e == "object")
        switch (e.$$typeof) {
        case Xf:
            return (e.displayName || "Context") + ".Consumer";
        case Yf:
            return (e._context.displayName || "Context") + ".Provider";
        case mu:
            var t = e.render;
            return e = e.displayName,
            e || (e = t.displayName || t.name || "",
            e = e !== "" ? "ForwardRef(" + e + ")" : "ForwardRef"),
            e;
        case vu:
            return t = e.displayName || null,
            t !== null ? t : ta(e.type) || "Memo";
        case ln:
            t = e._payload,
            e = e._init;
            try {
                return ta(e(t))
            } catch {}
        }
    return null
}
function Ig(e) {
    var t = e.type;
    switch (e.tag) {
    case 24:
        return "Cache";
    case 9:
        return (t.displayName || "Context") + ".Consumer";
    case 10:
        return (t._context.displayName || "Context") + ".Provider";
    case 18:
        return "DehydratedFragment";
    case 11:
        return e = t.render,
        e = e.displayName || e.name || "",
        t.displayName || (e !== "" ? "ForwardRef(" + e + ")" : "ForwardRef");
    case 7:
        return "Fragment";
    case 5:
        return t;
    case 4:
        return "Portal";
    case 3:
        return "Root";
    case 6:
        return "Text";
    case 16:
        return ta(t);
    case 8:
        return t === hu ? "StrictMode" : "Mode";
    case 22:
        return "Offscreen";
    case 12:
        return "Profiler";
    case 21:
        return "Scope";
    case 13:
        return "Suspense";
    case 19:
        return "SuspenseList";
    case 25:
        return "TracingMarker";
    case 1:
    case 0:
    case 17:
    case 2:
    case 14:
    case 15:
        if (typeof t == "function")
            return t.displayName || t.name || null;
        if (typeof t == "string")
            return t
    }
    return null
}
function Tn(e) {
    switch (typeof e) {
    case "boolean":
    case "number":
    case "string":
    case "undefined":
        return e;
    case "object":
        return e;
    default:
        return ""
    }
}
function Zf(e) {
    var t = e.type;
    return (e = e.nodeName) && e.toLowerCase() === "input" && (t === "checkbox" || t === "radio")
}
function Dg(e) {
    var t = Zf(e) ? "checked" : "value"
      , n = Object.getOwnPropertyDescriptor(e.constructor.prototype, t)
      , r = "" + e[t];
    if (!e.hasOwnProperty(t) && typeof n < "u" && typeof n.get == "function" && typeof n.set == "function") {
        var o = n.get
          , i = n.set;
        return Object.defineProperty(e, t, {
            configurable: !0,
            get: function() {
                return o.call(this)
            },
            set: function(s) {
                r = "" + s,
                i.call(this, s)
            }
        }),
        Object.defineProperty(e, t, {
            enumerable: n.enumerable
        }),
        {
            getValue: function() {
                return r
            },
            setValue: function(s) {
                r = "" + s
            },
            stopTracking: function() {
                e._valueTracker = null,
                delete e[t]
            }
        }
    }
}
function Ei(e) {
    e._valueTracker || (e._valueTracker = Dg(e))
}
function Jf(e) {
    if (!e)
        return !1;
    var t = e._valueTracker;
    if (!t)
        return !0;
    var n = t.getValue()
      , r = "";
    return e && (r = Zf(e) ? e.checked ? "true" : "false" : e.value),
    e = r,
    e !== n ? (t.setValue(e),
    !0) : !1
}
function rs(e) {
    if (e = e || (typeof document < "u" ? document : void 0),
    typeof e > "u")
        return null;
    try {
        return e.activeElement || e.body
    } catch {
        return e.body
    }
}
function na(e, t) {
    var n = t.checked;
    return de({}, t, {
        defaultChecked: void 0,
        defaultValue: void 0,
        value: void 0,
        checked: n ?? e._wrapperState.initialChecked
    })
}
function zc(e, t) {
    var n = t.defaultValue == null ? "" : t.defaultValue
      , r = t.checked != null ? t.checked : t.defaultChecked;
    n = Tn(t.value != null ? t.value : n),
    e._wrapperState = {
        initialChecked: r,
        initialValue: n,
        controlled: t.type === "checkbox" || t.type === "radio" ? t.checked != null : t.value != null
    }
}
function ep(e, t) {
    t = t.checked,
    t != null && pu(e, "checked", t, !1)
}
function ra(e, t) {
    ep(e, t);
    var n = Tn(t.value)
      , r = t.type;
    if (n != null)
        r === "number" ? (n === 0 && e.value === "" || e.value != n) && (e.value = "" + n) : e.value !== "" + n && (e.value = "" + n);
    else if (r === "submit" || r === "reset") {
        e.removeAttribute("value");
        return
    }
    t.hasOwnProperty("value") ? oa(e, t.type, n) : t.hasOwnProperty("defaultValue") && oa(e, t.type, Tn(t.defaultValue)),
    t.checked == null && t.defaultChecked != null && (e.defaultChecked = !!t.defaultChecked)
}
function Fc(e, t, n) {
    if (t.hasOwnProperty("value") || t.hasOwnProperty("defaultValue")) {
        var r = t.type;
        if (!(r !== "submit" && r !== "reset" || t.value !== void 0 && t.value !== null))
            return;
        t = "" + e._wrapperState.initialValue,
        n || t === e.value || (e.value = t),
        e.defaultValue = t
    }
    n = e.name,
    n !== "" && (e.name = ""),
    e.defaultChecked = !!e._wrapperState.initialChecked,
    n !== "" && (e.name = n)
}
function oa(e, t, n) {
    (t !== "number" || rs(e.ownerDocument) !== e) && (n == null ? e.defaultValue = "" + e._wrapperState.initialValue : e.defaultValue !== "" + n && (e.defaultValue = "" + n))
}
var wo = Array.isArray;
function br(e, t, n, r) {
    if (e = e.options,
    t) {
        t = {};
        for (var o = 0; o < n.length; o++)
            t["$" + n[o]] = !0;
        for (n = 0; n < e.length; n++)
            o = t.hasOwnProperty("$" + e[n].value),
            e[n].selected !== o && (e[n].selected = o),
            o && r && (e[n].defaultSelected = !0)
    } else {
        for (n = "" + Tn(n),
        t = null,
        o = 0; o < e.length; o++) {
            if (e[o].value === n) {
                e[o].selected = !0,
                r && (e[o].defaultSelected = !0);
                return
            }
            t !== null || e[o].disabled || (t = e[o])
        }
        t !== null && (t.selected = !0)
    }
}
function ia(e, t) {
    if (t.dangerouslySetInnerHTML != null)
        throw Error(A(91));
    return de({}, t, {
        value: void 0,
        defaultValue: void 0,
        children: "" + e._wrapperState.initialValue
    })
}
function $c(e, t) {
    var n = t.value;
    if (n == null) {
        if (n = t.children,
        t = t.defaultValue,
        n != null) {
            if (t != null)
                throw Error(A(92));
            if (wo(n)) {
                if (1 < n.length)
                    throw Error(A(93));
                n = n[0]
            }
            t = n
        }
        t == null && (t = ""),
        n = t
    }
    e._wrapperState = {
        initialValue: Tn(n)
    }
}
function tp(e, t) {
    var n = Tn(t.value)
      , r = Tn(t.defaultValue);
    n != null && (n = "" + n,
    n !== e.value && (e.value = n),
    t.defaultValue == null && e.defaultValue !== n && (e.defaultValue = n)),
    r != null && (e.defaultValue = "" + r)
}
function Uc(e) {
    var t = e.textContent;
    t === e._wrapperState.initialValue && t !== "" && t !== null && (e.value = t)
}
function np(e) {
    switch (e) {
    case "svg":
        return "http://www.w3.org/2000/svg";
    case "math":
        return "http://www.w3.org/1998/Math/MathML";
    default:
        return "http://www.w3.org/1999/xhtml"
    }
}
function sa(e, t) {
    return e == null || e === "http://www.w3.org/1999/xhtml" ? np(t) : e === "http://www.w3.org/2000/svg" && t === "foreignObject" ? "http://www.w3.org/1999/xhtml" : e
}
var Si, rp = function(e) {
    return typeof MSApp < "u" && MSApp.execUnsafeLocalFunction ? function(t, n, r, o) {
        MSApp.execUnsafeLocalFunction(function() {
            return e(t, n, r, o)
        })
    }
    : e
}(function(e, t) {
    if (e.namespaceURI !== "http://www.w3.org/2000/svg" || "innerHTML"in e)
        e.innerHTML = t;
    else {
        for (Si = Si || document.createElement("div"),
        Si.innerHTML = "<svg>" + t.valueOf().toString() + "</svg>",
        t = Si.firstChild; e.firstChild; )
            e.removeChild(e.firstChild);
        for (; t.firstChild; )
            e.appendChild(t.firstChild)
    }
});
function Lo(e, t) {
    if (t) {
        var n = e.firstChild;
        if (n && n === e.lastChild && n.nodeType === 3) {
            n.nodeValue = t;
            return
        }
    }
    e.textContent = t
}
var Co = {
    animationIterationCount: !0,
    aspectRatio: !0,
    borderImageOutset: !0,
    borderImageSlice: !0,
    borderImageWidth: !0,
    boxFlex: !0,
    boxFlexGroup: !0,
    boxOrdinalGroup: !0,
    columnCount: !0,
    columns: !0,
    flex: !0,
    flexGrow: !0,
    flexPositive: !0,
    flexShrink: !0,
    flexNegative: !0,
    flexOrder: !0,
    gridArea: !0,
    gridRow: !0,
    gridRowEnd: !0,
    gridRowSpan: !0,
    gridRowStart: !0,
    gridColumn: !0,
    gridColumnEnd: !0,
    gridColumnSpan: !0,
    gridColumnStart: !0,
    fontWeight: !0,
    lineClamp: !0,
    lineHeight: !0,
    opacity: !0,
    order: !0,
    orphans: !0,
    tabSize: !0,
    widows: !0,
    zIndex: !0,
    zoom: !0,
    fillOpacity: !0,
    floodOpacity: !0,
    stopOpacity: !0,
    strokeDasharray: !0,
    strokeDashoffset: !0,
    strokeMiterlimit: !0,
    strokeOpacity: !0,
    strokeWidth: !0
}
  , zg = ["Webkit", "ms", "Moz", "O"];
Object.keys(Co).forEach(function(e) {
    zg.forEach(function(t) {
        t = t + e.charAt(0).toUpperCase() + e.substring(1),
        Co[t] = Co[e]
    })
});
function op(e, t, n) {
    return t == null || typeof t == "boolean" || t === "" ? "" : n || typeof t != "number" || t === 0 || Co.hasOwnProperty(e) && Co[e] ? ("" + t).trim() : t + "px"
}
function ip(e, t) {
    e = e.style;
    for (var n in t)
        if (t.hasOwnProperty(n)) {
            var r = n.indexOf("--") === 0
              , o = op(n, t[n], r);
            n === "float" && (n = "cssFloat"),
            r ? e.setProperty(n, o) : e[n] = o
        }
}
var Fg = de({
    menuitem: !0
}, {
    area: !0,
    base: !0,
    br: !0,
    col: !0,
    embed: !0,
    hr: !0,
    img: !0,
    input: !0,
    keygen: !0,
    link: !0,
    meta: !0,
    param: !0,
    source: !0,
    track: !0,
    wbr: !0
});
function la(e, t) {
    if (t) {
        if (Fg[e] && (t.children != null || t.dangerouslySetInnerHTML != null))
            throw Error(A(137, e));
        if (t.dangerouslySetInnerHTML != null) {
            if (t.children != null)
                throw Error(A(60));
            if (typeof t.dangerouslySetInnerHTML != "object" || !("__html"in t.dangerouslySetInnerHTML))
                throw Error(A(61))
        }
        if (t.style != null && typeof t.style != "object")
            throw Error(A(62))
    }
}
function aa(e, t) {
    if (e.indexOf("-") === -1)
        return typeof t.is == "string";
    switch (e) {
    case "annotation-xml":
    case "color-profile":
    case "font-face":
    case "font-face-src":
    case "font-face-uri":
    case "font-face-format":
    case "font-face-name":
    case "missing-glyph":
        return !1;
    default:
        return !0
    }
}
var ua = null;
function gu(e) {
    return e = e.target || e.srcElement || window,
    e.correspondingUseElement && (e = e.correspondingUseElement),
    e.nodeType === 3 ? e.parentNode : e
}
var ca = null
  , kr = null
  , Pr = null;
function Bc(e) {
    if (e = ai(e)) {
        if (typeof ca != "function")
            throw Error(A(280));
        var t = e.stateNode;
        t && (t = $s(t),
        ca(e.stateNode, e.type, t))
    }
}
function sp(e) {
    kr ? Pr ? Pr.push(e) : Pr = [e] : kr = e
}
function lp() {
    if (kr) {
        var e = kr
          , t = Pr;
        if (Pr = kr = null,
        Bc(e),
        t)
            for (e = 0; e < t.length; e++)
                Bc(t[e])
    }
}
function ap(e, t) {
    return e(t)
}
function up() {}
var El = !1;
function cp(e, t, n) {
    if (El)
        return e(t, n);
    El = !0;
    try {
        return ap(e, t, n)
    } finally {
        El = !1,
        (kr !== null || Pr !== null) && (up(),
        lp())
    }
}
function Mo(e, t) {
    var n = e.stateNode;
    if (n === null)
        return null;
    var r = $s(n);
    if (r === null)
        return null;
    n = r[t];
    e: switch (t) {
    case "onClick":
    case "onClickCapture":
    case "onDoubleClick":
    case "onDoubleClickCapture":
    case "onMouseDown":
    case "onMouseDownCapture":
    case "onMouseMove":
    case "onMouseMoveCapture":
    case "onMouseUp":
    case "onMouseUpCapture":
    case "onMouseEnter":
        (r = !r.disabled) || (e = e.type,
        r = !(e === "button" || e === "input" || e === "select" || e === "textarea")),
        e = !r;
        break e;
    default:
        e = !1
    }
    if (e)
        return null;
    if (n && typeof n != "function")
        throw Error(A(231, t, typeof n));
    return n
}
var da = !1;
if (Wt)
    try {
        var ao = {};
        Object.defineProperty(ao, "passive", {
            get: function() {
                da = !0
            }
        }),
        window.addEventListener("test", ao, ao),
        window.removeEventListener("test", ao, ao)
    } catch {
        da = !1
    }
function $g(e, t, n, r, o, i, s, l, a) {
    var u = Array.prototype.slice.call(arguments, 3);
    try {
        t.apply(n, u)
    } catch (d) {
        this.onError(d)
    }
}
var bo = !1
  , os = null
  , is = !1
  , fa = null
  , Ug = {
    onError: function(e) {
        bo = !0,
        os = e
    }
};
function Bg(e, t, n, r, o, i, s, l, a) {
    bo = !1,
    os = null,
    $g.apply(Ug, arguments)
}
function Wg(e, t, n, r, o, i, s, l, a) {
    if (Bg.apply(this, arguments),
    bo) {
        if (bo) {
            var u = os;
            bo = !1,
            os = null
        } else
            throw Error(A(198));
        is || (is = !0,
        fa = u)
    }
}
function or(e) {
    var t = e
      , n = e;
    if (e.alternate)
        for (; t.return; )
            t = t.return;
    else {
        e = t;
        do
            t = e,
            t.flags & 4098 && (n = t.return),
            e = t.return;
        while (e)
    }
    return t.tag === 3 ? n : null
}
function dp(e) {
    if (e.tag === 13) {
        var t = e.memoizedState;
        if (t === null && (e = e.alternate,
        e !== null && (t = e.memoizedState)),
        t !== null)
            return t.dehydrated
    }
    return null
}
function Wc(e) {
    if (or(e) !== e)
        throw Error(A(188))
}
function Vg(e) {
    var t = e.alternate;
    if (!t) {
        if (t = or(e),
        t === null)
            throw Error(A(188));
        return t !== e ? null : e
    }
    for (var n = e, r = t; ; ) {
        var o = n.return;
        if (o === null)
            break;
        var i = o.alternate;
        if (i === null) {
            if (r = o.return,
            r !== null) {
                n = r;
                continue
            }
            break
        }
        if (o.child === i.child) {
            for (i = o.child; i; ) {
                if (i === n)
                    return Wc(o),
                    e;
                if (i === r)
                    return Wc(o),
                    t;
                i = i.sibling
            }
            throw Error(A(188))
        }
        if (n.return !== r.return)
            n = o,
            r = i;
        else {
            for (var s = !1, l = o.child; l; ) {
                if (l === n) {
                    s = !0,
                    n = o,
                    r = i;
                    break
                }
                if (l === r) {
                    s = !0,
                    r = o,
                    n = i;
                    break
                }
                l = l.sibling
            }
            if (!s) {
                for (l = i.child; l; ) {
                    if (l === n) {
                        s = !0,
                        n = i,
                        r = o;
                        break
                    }
                    if (l === r) {
                        s = !0,
                        r = i,
                        n = o;
                        break
                    }
                    l = l.sibling
                }
                if (!s)
                    throw Error(A(189))
            }
        }
        if (n.alternate !== r)
            throw Error(A(190))
    }
    if (n.tag !== 3)
        throw Error(A(188));
    return n.stateNode.current === n ? e : t
}
function fp(e) {
    return e = Vg(e),
    e !== null ? pp(e) : null
}
function pp(e) {
    if (e.tag === 5 || e.tag === 6)
        return e;
    for (e = e.child; e !== null; ) {
        var t = pp(e);
        if (t !== null)
            return t;
        e = e.sibling
    }
    return null
}
var hp = Ze.unstable_scheduleCallback
  , Vc = Ze.unstable_cancelCallback
  , Hg = Ze.unstable_shouldYield
  , Qg = Ze.unstable_requestPaint
  , he = Ze.unstable_now
  , Kg = Ze.unstable_getCurrentPriorityLevel
  , yu = Ze.unstable_ImmediatePriority
  , mp = Ze.unstable_UserBlockingPriority
  , ss = Ze.unstable_NormalPriority
  , Gg = Ze.unstable_LowPriority
  , vp = Ze.unstable_IdlePriority
  , Is = null
  , Ot = null;
function Yg(e) {
    if (Ot && typeof Ot.onCommitFiberRoot == "function")
        try {
            Ot.onCommitFiberRoot(Is, e, void 0, (e.current.flags & 128) === 128)
        } catch {}
}
var vt = Math.clz32 ? Math.clz32 : Zg
  , Xg = Math.log
  , qg = Math.LN2;
function Zg(e) {
    return e >>>= 0,
    e === 0 ? 32 : 31 - (Xg(e) / qg | 0) | 0
}
var Ci = 64
  , bi = 4194304;
function Eo(e) {
    switch (e & -e) {
    case 1:
        return 1;
    case 2:
        return 2;
    case 4:
        return 4;
    case 8:
        return 8;
    case 16:
        return 16;
    case 32:
        return 32;
    case 64:
    case 128:
    case 256:
    case 512:
    case 1024:
    case 2048:
    case 4096:
    case 8192:
    case 16384:
    case 32768:
    case 65536:
    case 131072:
    case 262144:
    case 524288:
    case 1048576:
    case 2097152:
        return e & 4194240;
    case 4194304:
    case 8388608:
    case 16777216:
    case 33554432:
    case 67108864:
        return e & 130023424;
    case 134217728:
        return 134217728;
    case 268435456:
        return 268435456;
    case 536870912:
        return 536870912;
    case 1073741824:
        return 1073741824;
    default:
        return e
    }
}
function ls(e, t) {
    var n = e.pendingLanes;
    if (n === 0)
        return 0;
    var r = 0
      , o = e.suspendedLanes
      , i = e.pingedLanes
      , s = n & 268435455;
    if (s !== 0) {
        var l = s & ~o;
        l !== 0 ? r = Eo(l) : (i &= s,
        i !== 0 && (r = Eo(i)))
    } else
        s = n & ~o,
        s !== 0 ? r = Eo(s) : i !== 0 && (r = Eo(i));
    if (r === 0)
        return 0;
    if (t !== 0 && t !== r && !(t & o) && (o = r & -r,
    i = t & -t,
    o >= i || o === 16 && (i & 4194240) !== 0))
        return t;
    if (r & 4 && (r |= n & 16),
    t = e.entangledLanes,
    t !== 0)
        for (e = e.entanglements,
        t &= r; 0 < t; )
            n = 31 - vt(t),
            o = 1 << n,
            r |= e[n],
            t &= ~o;
    return r
}
function Jg(e, t) {
    switch (e) {
    case 1:
    case 2:
    case 4:
        return t + 250;
    case 8:
    case 16:
    case 32:
    case 64:
    case 128:
    case 256:
    case 512:
    case 1024:
    case 2048:
    case 4096:
    case 8192:
    case 16384:
    case 32768:
    case 65536:
    case 131072:
    case 262144:
    case 524288:
    case 1048576:
    case 2097152:
        return t + 5e3;
    case 4194304:
    case 8388608:
    case 16777216:
    case 33554432:
    case 67108864:
        return -1;
    case 134217728:
    case 268435456:
    case 536870912:
    case 1073741824:
        return -1;
    default:
        return -1
    }
}
function ey(e, t) {
    for (var n = e.suspendedLanes, r = e.pingedLanes, o = e.expirationTimes, i = e.pendingLanes; 0 < i; ) {
        var s = 31 - vt(i)
          , l = 1 << s
          , a = o[s];
        a === -1 ? (!(l & n) || l & r) && (o[s] = Jg(l, t)) : a <= t && (e.expiredLanes |= l),
        i &= ~l
    }
}
function pa(e) {
    return e = e.pendingLanes & -1073741825,
    e !== 0 ? e : e & 1073741824 ? 1073741824 : 0
}
function gp() {
    var e = Ci;
    return Ci <<= 1,
    !(Ci & 4194240) && (Ci = 64),
    e
}
function Sl(e) {
    for (var t = [], n = 0; 31 > n; n++)
        t.push(e);
    return t
}
function si(e, t, n) {
    e.pendingLanes |= t,
    t !== 536870912 && (e.suspendedLanes = 0,
    e.pingedLanes = 0),
    e = e.eventTimes,
    t = 31 - vt(t),
    e[t] = n
}
function ty(e, t) {
    var n = e.pendingLanes & ~t;
    e.pendingLanes = t,
    e.suspendedLanes = 0,
    e.pingedLanes = 0,
    e.expiredLanes &= t,
    e.mutableReadLanes &= t,
    e.entangledLanes &= t,
    t = e.entanglements;
    var r = e.eventTimes;
    for (e = e.expirationTimes; 0 < n; ) {
        var o = 31 - vt(n)
          , i = 1 << o;
        t[o] = 0,
        r[o] = -1,
        e[o] = -1,
        n &= ~i
    }
}
function xu(e, t) {
    var n = e.entangledLanes |= t;
    for (e = e.entanglements; n; ) {
        var r = 31 - vt(n)
          , o = 1 << r;
        o & t | e[r] & t && (e[r] |= t),
        n &= ~o
    }
}
var J = 0;
function yp(e) {
    return e &= -e,
    1 < e ? 4 < e ? e & 268435455 ? 16 : 536870912 : 4 : 1
}
var xp, wu, wp, Ep, Sp, ha = !1, ki = [], xn = null, wn = null, En = null, Io = new Map, Do = new Map, un = [], ny = "mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset submit".split(" ");
function Hc(e, t) {
    switch (e) {
    case "focusin":
    case "focusout":
        xn = null;
        break;
    case "dragenter":
    case "dragleave":
        wn = null;
        break;
    case "mouseover":
    case "mouseout":
        En = null;
        break;
    case "pointerover":
    case "pointerout":
        Io.delete(t.pointerId);
        break;
    case "gotpointercapture":
    case "lostpointercapture":
        Do.delete(t.pointerId)
    }
}
function uo(e, t, n, r, o, i) {
    return e === null || e.nativeEvent !== i ? (e = {
        blockedOn: t,
        domEventName: n,
        eventSystemFlags: r,
        nativeEvent: i,
        targetContainers: [o]
    },
    t !== null && (t = ai(t),
    t !== null && wu(t)),
    e) : (e.eventSystemFlags |= r,
    t = e.targetContainers,
    o !== null && t.indexOf(o) === -1 && t.push(o),
    e)
}
function ry(e, t, n, r, o) {
    switch (t) {
    case "focusin":
        return xn = uo(xn, e, t, n, r, o),
        !0;
    case "dragenter":
        return wn = uo(wn, e, t, n, r, o),
        !0;
    case "mouseover":
        return En = uo(En, e, t, n, r, o),
        !0;
    case "pointerover":
        var i = o.pointerId;
        return Io.set(i, uo(Io.get(i) || null, e, t, n, r, o)),
        !0;
    case "gotpointercapture":
        return i = o.pointerId,
        Do.set(i, uo(Do.get(i) || null, e, t, n, r, o)),
        !0
    }
    return !1
}
function Cp(e) {
    var t = Bn(e.target);
    if (t !== null) {
        var n = or(t);
        if (n !== null) {
            if (t = n.tag,
            t === 13) {
                if (t = dp(n),
                t !== null) {
                    e.blockedOn = t,
                    Sp(e.priority, function() {
                        wp(n)
                    });
                    return
                }
            } else if (t === 3 && n.stateNode.current.memoizedState.isDehydrated) {
                e.blockedOn = n.tag === 3 ? n.stateNode.containerInfo : null;
                return
            }
        }
    }
    e.blockedOn = null
}
function Vi(e) {
    if (e.blockedOn !== null)
        return !1;
    for (var t = e.targetContainers; 0 < t.length; ) {
        var n = ma(e.domEventName, e.eventSystemFlags, t[0], e.nativeEvent);
        if (n === null) {
            n = e.nativeEvent;
            var r = new n.constructor(n.type,n);
            ua = r,
            n.target.dispatchEvent(r),
            ua = null
        } else
            return t = ai(n),
            t !== null && wu(t),
            e.blockedOn = n,
            !1;
        t.shift()
    }
    return !0
}
function Qc(e, t, n) {
    Vi(e) && n.delete(t)
}
function oy() {
    ha = !1,
    xn !== null && Vi(xn) && (xn = null),
    wn !== null && Vi(wn) && (wn = null),
    En !== null && Vi(En) && (En = null),
    Io.forEach(Qc),
    Do.forEach(Qc)
}
function co(e, t) {
    e.blockedOn === t && (e.blockedOn = null,
    ha || (ha = !0,
    Ze.unstable_scheduleCallback(Ze.unstable_NormalPriority, oy)))
}
function zo(e) {
    function t(o) {
        return co(o, e)
    }
    if (0 < ki.length) {
        co(ki[0], e);
        for (var n = 1; n < ki.length; n++) {
            var r = ki[n];
            r.blockedOn === e && (r.blockedOn = null)
        }
    }
    for (xn !== null && co(xn, e),
    wn !== null && co(wn, e),
    En !== null && co(En, e),
    Io.forEach(t),
    Do.forEach(t),
    n = 0; n < un.length; n++)
        r = un[n],
        r.blockedOn === e && (r.blockedOn = null);
    for (; 0 < un.length && (n = un[0],
    n.blockedOn === null); )
        Cp(n),
        n.blockedOn === null && un.shift()
}
var Nr = Yt.ReactCurrentBatchConfig
  , as = !0;
function iy(e, t, n, r) {
    var o = J
      , i = Nr.transition;
    Nr.transition = null;
    try {
        J = 1,
        Eu(e, t, n, r)
    } finally {
        J = o,
        Nr.transition = i
    }
}
function sy(e, t, n, r) {
    var o = J
      , i = Nr.transition;
    Nr.transition = null;
    try {
        J = 4,
        Eu(e, t, n, r)
    } finally {
        J = o,
        Nr.transition = i
    }
}
function Eu(e, t, n, r) {
    if (as) {
        var o = ma(e, t, n, r);
        if (o === null)
            Ol(e, t, r, us, n),
            Hc(e, r);
        else if (ry(o, e, t, n, r))
            r.stopPropagation();
        else if (Hc(e, r),
        t & 4 && -1 < ny.indexOf(e)) {
            for (; o !== null; ) {
                var i = ai(o);
                if (i !== null && xp(i),
                i = ma(e, t, n, r),
                i === null && Ol(e, t, r, us, n),
                i === o)
                    break;
                o = i
            }
            o !== null && r.stopPropagation()
        } else
            Ol(e, t, r, null, n)
    }
}
var us = null;
function ma(e, t, n, r) {
    if (us = null,
    e = gu(r),
    e = Bn(e),
    e !== null)
        if (t = or(e),
        t === null)
            e = null;
        else if (n = t.tag,
        n === 13) {
            if (e = dp(t),
            e !== null)
                return e;
            e = null
        } else if (n === 3) {
            if (t.stateNode.current.memoizedState.isDehydrated)
                return t.tag === 3 ? t.stateNode.containerInfo : null;
            e = null
        } else
            t !== e && (e = null);
    return us = e,
    null
}
function bp(e) {
    switch (e) {
    case "cancel":
    case "click":
    case "close":
    case "contextmenu":
    case "copy":
    case "cut":
    case "auxclick":
    case "dblclick":
    case "dragend":
    case "dragstart":
    case "drop":
    case "focusin":
    case "focusout":
    case "input":
    case "invalid":
    case "keydown":
    case "keypress":
    case "keyup":
    case "mousedown":
    case "mouseup":
    case "paste":
    case "pause":
    case "play":
    case "pointercancel":
    case "pointerdown":
    case "pointerup":
    case "ratechange":
    case "reset":
    case "resize":
    case "seeked":
    case "submit":
    case "touchcancel":
    case "touchend":
    case "touchstart":
    case "volumechange":
    case "change":
    case "selectionchange":
    case "textInput":
    case "compositionstart":
    case "compositionend":
    case "compositionupdate":
    case "beforeblur":
    case "afterblur":
    case "beforeinput":
    case "blur":
    case "fullscreenchange":
    case "focus":
    case "hashchange":
    case "popstate":
    case "select":
    case "selectstart":
        return 1;
    case "drag":
    case "dragenter":
    case "dragexit":
    case "dragleave":
    case "dragover":
    case "mousemove":
    case "mouseout":
    case "mouseover":
    case "pointermove":
    case "pointerout":
    case "pointerover":
    case "scroll":
    case "toggle":
    case "touchmove":
    case "wheel":
    case "mouseenter":
    case "mouseleave":
    case "pointerenter":
    case "pointerleave":
        return 4;
    case "message":
        switch (Kg()) {
        case yu:
            return 1;
        case mp:
            return 4;
        case ss:
        case Gg:
            return 16;
        case vp:
            return 536870912;
        default:
            return 16
        }
    default:
        return 16
    }
}
var vn = null
  , Su = null
  , Hi = null;
function kp() {
    if (Hi)
        return Hi;
    var e, t = Su, n = t.length, r, o = "value"in vn ? vn.value : vn.textContent, i = o.length;
    for (e = 0; e < n && t[e] === o[e]; e++)
        ;
    var s = n - e;
    for (r = 1; r <= s && t[n - r] === o[i - r]; r++)
        ;
    return Hi = o.slice(e, 1 < r ? 1 - r : void 0)
}
function Qi(e) {
    var t = e.keyCode;
    return "charCode"in e ? (e = e.charCode,
    e === 0 && t === 13 && (e = 13)) : e = t,
    e === 10 && (e = 13),
    32 <= e || e === 13 ? e : 0
}
function Pi() {
    return !0
}
function Kc() {
    return !1
}
function et(e) {
    function t(n, r, o, i, s) {
        this._reactName = n,
        this._targetInst = o,
        this.type = r,
        this.nativeEvent = i,
        this.target = s,
        this.currentTarget = null;
        for (var l in e)
            e.hasOwnProperty(l) && (n = e[l],
            this[l] = n ? n(i) : i[l]);
        return this.isDefaultPrevented = (i.defaultPrevented != null ? i.defaultPrevented : i.returnValue === !1) ? Pi : Kc,
        this.isPropagationStopped = Kc,
        this
    }
    return de(t.prototype, {
        preventDefault: function() {
            this.defaultPrevented = !0;
            var n = this.nativeEvent;
            n && (n.preventDefault ? n.preventDefault() : typeof n.returnValue != "unknown" && (n.returnValue = !1),
            this.isDefaultPrevented = Pi)
        },
        stopPropagation: function() {
            var n = this.nativeEvent;
            n && (n.stopPropagation ? n.stopPropagation() : typeof n.cancelBubble != "unknown" && (n.cancelBubble = !0),
            this.isPropagationStopped = Pi)
        },
        persist: function() {},
        isPersistent: Pi
    }),
    t
}
var qr = {
    eventPhase: 0,
    bubbles: 0,
    cancelable: 0,
    timeStamp: function(e) {
        return e.timeStamp || Date.now()
    },
    defaultPrevented: 0,
    isTrusted: 0
}, Cu = et(qr), li = de({}, qr, {
    view: 0,
    detail: 0
}), ly = et(li), Cl, bl, fo, Ds = de({}, li, {
    screenX: 0,
    screenY: 0,
    clientX: 0,
    clientY: 0,
    pageX: 0,
    pageY: 0,
    ctrlKey: 0,
    shiftKey: 0,
    altKey: 0,
    metaKey: 0,
    getModifierState: bu,
    button: 0,
    buttons: 0,
    relatedTarget: function(e) {
        return e.relatedTarget === void 0 ? e.fromElement === e.srcElement ? e.toElement : e.fromElement : e.relatedTarget
    },
    movementX: function(e) {
        return "movementX"in e ? e.movementX : (e !== fo && (fo && e.type === "mousemove" ? (Cl = e.screenX - fo.screenX,
        bl = e.screenY - fo.screenY) : bl = Cl = 0,
        fo = e),
        Cl)
    },
    movementY: function(e) {
        return "movementY"in e ? e.movementY : bl
    }
}), Gc = et(Ds), ay = de({}, Ds, {
    dataTransfer: 0
}), uy = et(ay), cy = de({}, li, {
    relatedTarget: 0
}), kl = et(cy), dy = de({}, qr, {
    animationName: 0,
    elapsedTime: 0,
    pseudoElement: 0
}), fy = et(dy), py = de({}, qr, {
    clipboardData: function(e) {
        return "clipboardData"in e ? e.clipboardData : window.clipboardData
    }
}), hy = et(py), my = de({}, qr, {
    data: 0
}), Yc = et(my), vy = {
    Esc: "Escape",
    Spacebar: " ",
    Left: "ArrowLeft",
    Up: "ArrowUp",
    Right: "ArrowRight",
    Down: "ArrowDown",
    Del: "Delete",
    Win: "OS",
    Menu: "ContextMenu",
    Apps: "ContextMenu",
    Scroll: "ScrollLock",
    MozPrintableKey: "Unidentified"
}, gy = {
    8: "Backspace",
    9: "Tab",
    12: "Clear",
    13: "Enter",
    16: "Shift",
    17: "Control",
    18: "Alt",
    19: "Pause",
    20: "CapsLock",
    27: "Escape",
    32: " ",
    33: "PageUp",
    34: "PageDown",
    35: "End",
    36: "Home",
    37: "ArrowLeft",
    38: "ArrowUp",
    39: "ArrowRight",
    40: "ArrowDown",
    45: "Insert",
    46: "Delete",
    112: "F1",
    113: "F2",
    114: "F3",
    115: "F4",
    116: "F5",
    117: "F6",
    118: "F7",
    119: "F8",
    120: "F9",
    121: "F10",
    122: "F11",
    123: "F12",
    144: "NumLock",
    145: "ScrollLock",
    224: "Meta"
}, yy = {
    Alt: "altKey",
    Control: "ctrlKey",
    Meta: "metaKey",
    Shift: "shiftKey"
};
function xy(e) {
    var t = this.nativeEvent;
    return t.getModifierState ? t.getModifierState(e) : (e = yy[e]) ? !!t[e] : !1
}
function bu() {
    return xy
}
var wy = de({}, li, {
    key: function(e) {
        if (e.key) {
            var t = vy[e.key] || e.key;
            if (t !== "Unidentified")
                return t
        }
        return e.type === "keypress" ? (e = Qi(e),
        e === 13 ? "Enter" : String.fromCharCode(e)) : e.type === "keydown" || e.type === "keyup" ? gy[e.keyCode] || "Unidentified" : ""
    },
    code: 0,
    location: 0,
    ctrlKey: 0,
    shiftKey: 0,
    altKey: 0,
    metaKey: 0,
    repeat: 0,
    locale: 0,
    getModifierState: bu,
    charCode: function(e) {
        return e.type === "keypress" ? Qi(e) : 0
    },
    keyCode: function(e) {
        return e.type === "keydown" || e.type === "keyup" ? e.keyCode : 0
    },
    which: function(e) {
        return e.type === "keypress" ? Qi(e) : e.type === "keydown" || e.type === "keyup" ? e.keyCode : 0
    }
})
  , Ey = et(wy)
  , Sy = de({}, Ds, {
    pointerId: 0,
    width: 0,
    height: 0,
    pressure: 0,
    tangentialPressure: 0,
    tiltX: 0,
    tiltY: 0,
    twist: 0,
    pointerType: 0,
    isPrimary: 0
})
  , Xc = et(Sy)
  , Cy = de({}, li, {
    touches: 0,
    targetTouches: 0,
    changedTouches: 0,
    altKey: 0,
    metaKey: 0,
    ctrlKey: 0,
    shiftKey: 0,
    getModifierState: bu
})
  , by = et(Cy)
  , ky = de({}, qr, {
    propertyName: 0,
    elapsedTime: 0,
    pseudoElement: 0
})
  , Py = et(ky)
  , Ny = de({}, Ds, {
    deltaX: function(e) {
        return "deltaX"in e ? e.deltaX : "wheelDeltaX"in e ? -e.wheelDeltaX : 0
    },
    deltaY: function(e) {
        return "deltaY"in e ? e.deltaY : "wheelDeltaY"in e ? -e.wheelDeltaY : "wheelDelta"in e ? -e.wheelDelta : 0
    },
    deltaZ: 0,
    deltaMode: 0
})
  , Ty = et(Ny)
  , Ry = [9, 13, 27, 32]
  , ku = Wt && "CompositionEvent"in window
  , ko = null;
Wt && "documentMode"in document && (ko = document.documentMode);
var Ay = Wt && "TextEvent"in window && !ko
  , Pp = Wt && (!ku || ko && 8 < ko && 11 >= ko)
  , qc = " "
  , Zc = !1;
function Np(e, t) {
    switch (e) {
    case "keyup":
        return Ry.indexOf(t.keyCode) !== -1;
    case "keydown":
        return t.keyCode !== 229;
    case "keypress":
    case "mousedown":
    case "focusout":
        return !0;
    default:
        return !1
    }
}
function Tp(e) {
    return e = e.detail,
    typeof e == "object" && "data"in e ? e.data : null
}
var hr = !1;
function jy(e, t) {
    switch (e) {
    case "compositionend":
        return Tp(t);
    case "keypress":
        return t.which !== 32 ? null : (Zc = !0,
        qc);
    case "textInput":
        return e = t.data,
        e === qc && Zc ? null : e;
    default:
        return null
    }
}
function Oy(e, t) {
    if (hr)
        return e === "compositionend" || !ku && Np(e, t) ? (e = kp(),
        Hi = Su = vn = null,
        hr = !1,
        e) : null;
    switch (e) {
    case "paste":
        return null;
    case "keypress":
        if (!(t.ctrlKey || t.altKey || t.metaKey) || t.ctrlKey && t.altKey) {
            if (t.char && 1 < t.char.length)
                return t.char;
            if (t.which)
                return String.fromCharCode(t.which)
        }
        return null;
    case "compositionend":
        return Pp && t.locale !== "ko" ? null : t.data;
    default:
        return null
    }
}
var _y = {
    color: !0,
    date: !0,
    datetime: !0,
    "datetime-local": !0,
    email: !0,
    month: !0,
    number: !0,
    password: !0,
    range: !0,
    search: !0,
    tel: !0,
    text: !0,
    time: !0,
    url: !0,
    week: !0
};
function Jc(e) {
    var t = e && e.nodeName && e.nodeName.toLowerCase();
    return t === "input" ? !!_y[e.type] : t === "textarea"
}
function Rp(e, t, n, r) {
    sp(r),
    t = cs(t, "onChange"),
    0 < t.length && (n = new Cu("onChange","change",null,n,r),
    e.push({
        event: n,
        listeners: t
    }))
}
var Po = null
  , Fo = null;
function Ly(e) {
    $p(e, 0)
}
function zs(e) {
    var t = gr(e);
    if (Jf(t))
        return e
}
function My(e, t) {
    if (e === "change")
        return t
}
var Ap = !1;
if (Wt) {
    var Pl;
    if (Wt) {
        var Nl = "oninput"in document;
        if (!Nl) {
            var ed = document.createElement("div");
            ed.setAttribute("oninput", "return;"),
            Nl = typeof ed.oninput == "function"
        }
        Pl = Nl
    } else
        Pl = !1;
    Ap = Pl && (!document.documentMode || 9 < document.documentMode)
}
function td() {
    Po && (Po.detachEvent("onpropertychange", jp),
    Fo = Po = null)
}
function jp(e) {
    if (e.propertyName === "value" && zs(Fo)) {
        var t = [];
        Rp(t, Fo, e, gu(e)),
        cp(Ly, t)
    }
}
function Iy(e, t, n) {
    e === "focusin" ? (td(),
    Po = t,
    Fo = n,
    Po.attachEvent("onpropertychange", jp)) : e === "focusout" && td()
}
function Dy(e) {
    if (e === "selectionchange" || e === "keyup" || e === "keydown")
        return zs(Fo)
}
function zy(e, t) {
    if (e === "click")
        return zs(t)
}
function Fy(e, t) {
    if (e === "input" || e === "change")
        return zs(t)
}
function $y(e, t) {
    return e === t && (e !== 0 || 1 / e === 1 / t) || e !== e && t !== t
}
var yt = typeof Object.is == "function" ? Object.is : $y;
function $o(e, t) {
    if (yt(e, t))
        return !0;
    if (typeof e != "object" || e === null || typeof t != "object" || t === null)
        return !1;
    var n = Object.keys(e)
      , r = Object.keys(t);
    if (n.length !== r.length)
        return !1;
    for (r = 0; r < n.length; r++) {
        var o = n[r];
        if (!ql.call(t, o) || !yt(e[o], t[o]))
            return !1
    }
    return !0
}
function nd(e) {
    for (; e && e.firstChild; )
        e = e.firstChild;
    return e
}
function rd(e, t) {
    var n = nd(e);
    e = 0;
    for (var r; n; ) {
        if (n.nodeType === 3) {
            if (r = e + n.textContent.length,
            e <= t && r >= t)
                return {
                    node: n,
                    offset: t - e
                };
            e = r
        }
        e: {
            for (; n; ) {
                if (n.nextSibling) {
                    n = n.nextSibling;
                    break e
                }
                n = n.parentNode
            }
            n = void 0
        }
        n = nd(n)
    }
}
function Op(e, t) {
    return e && t ? e === t ? !0 : e && e.nodeType === 3 ? !1 : t && t.nodeType === 3 ? Op(e, t.parentNode) : "contains"in e ? e.contains(t) : e.compareDocumentPosition ? !!(e.compareDocumentPosition(t) & 16) : !1 : !1
}
function _p() {
    for (var e = window, t = rs(); t instanceof e.HTMLIFrameElement; ) {
        try {
            var n = typeof t.contentWindow.location.href == "string"
        } catch {
            n = !1
        }
        if (n)
            e = t.contentWindow;
        else
            break;
        t = rs(e.document)
    }
    return t
}
function Pu(e) {
    var t = e && e.nodeName && e.nodeName.toLowerCase();
    return t && (t === "input" && (e.type === "text" || e.type === "search" || e.type === "tel" || e.type === "url" || e.type === "password") || t === "textarea" || e.contentEditable === "true")
}
function Uy(e) {
    var t = _p()
      , n = e.focusedElem
      , r = e.selectionRange;
    if (t !== n && n && n.ownerDocument && Op(n.ownerDocument.documentElement, n)) {
        if (r !== null && Pu(n)) {
            if (t = r.start,
            e = r.end,
            e === void 0 && (e = t),
            "selectionStart"in n)
                n.selectionStart = t,
                n.selectionEnd = Math.min(e, n.value.length);
            else if (e = (t = n.ownerDocument || document) && t.defaultView || window,
            e.getSelection) {
                e = e.getSelection();
                var o = n.textContent.length
                  , i = Math.min(r.start, o);
                r = r.end === void 0 ? i : Math.min(r.end, o),
                !e.extend && i > r && (o = r,
                r = i,
                i = o),
                o = rd(n, i);
                var s = rd(n, r);
                o && s && (e.rangeCount !== 1 || e.anchorNode !== o.node || e.anchorOffset !== o.offset || e.focusNode !== s.node || e.focusOffset !== s.offset) && (t = t.createRange(),
                t.setStart(o.node, o.offset),
                e.removeAllRanges(),
                i > r ? (e.addRange(t),
                e.extend(s.node, s.offset)) : (t.setEnd(s.node, s.offset),
                e.addRange(t)))
            }
        }
        for (t = [],
        e = n; e = e.parentNode; )
            e.nodeType === 1 && t.push({
                element: e,
                left: e.scrollLeft,
                top: e.scrollTop
            });
        for (typeof n.focus == "function" && n.focus(),
        n = 0; n < t.length; n++)
            e = t[n],
            e.element.scrollLeft = e.left,
            e.element.scrollTop = e.top
    }
}
var By = Wt && "documentMode"in document && 11 >= document.documentMode
  , mr = null
  , va = null
  , No = null
  , ga = !1;
function od(e, t, n) {
    var r = n.window === n ? n.document : n.nodeType === 9 ? n : n.ownerDocument;
    ga || mr == null || mr !== rs(r) || (r = mr,
    "selectionStart"in r && Pu(r) ? r = {
        start: r.selectionStart,
        end: r.selectionEnd
    } : (r = (r.ownerDocument && r.ownerDocument.defaultView || window).getSelection(),
    r = {
        anchorNode: r.anchorNode,
        anchorOffset: r.anchorOffset,
        focusNode: r.focusNode,
        focusOffset: r.focusOffset
    }),
    No && $o(No, r) || (No = r,
    r = cs(va, "onSelect"),
    0 < r.length && (t = new Cu("onSelect","select",null,t,n),
    e.push({
        event: t,
        listeners: r
    }),
    t.target = mr)))
}
function Ni(e, t) {
    var n = {};
    return n[e.toLowerCase()] = t.toLowerCase(),
    n["Webkit" + e] = "webkit" + t,
    n["Moz" + e] = "moz" + t,
    n
}
var vr = {
    animationend: Ni("Animation", "AnimationEnd"),
    animationiteration: Ni("Animation", "AnimationIteration"),
    animationstart: Ni("Animation", "AnimationStart"),
    transitionend: Ni("Transition", "TransitionEnd")
}
  , Tl = {}
  , Lp = {};
Wt && (Lp = document.createElement("div").style,
"AnimationEvent"in window || (delete vr.animationend.animation,
delete vr.animationiteration.animation,
delete vr.animationstart.animation),
"TransitionEvent"in window || delete vr.transitionend.transition);
function Fs(e) {
    if (Tl[e])
        return Tl[e];
    if (!vr[e])
        return e;
    var t = vr[e], n;
    for (n in t)
        if (t.hasOwnProperty(n) && n in Lp)
            return Tl[e] = t[n];
    return e
}
var Mp = Fs("animationend")
  , Ip = Fs("animationiteration")
  , Dp = Fs("animationstart")
  , zp = Fs("transitionend")
  , Fp = new Map
  , id = "abort auxClick cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");
function Ln(e, t) {
    Fp.set(e, t),
    rr(t, [e])
}
for (var Rl = 0; Rl < id.length; Rl++) {
    var Al = id[Rl]
      , Wy = Al.toLowerCase()
      , Vy = Al[0].toUpperCase() + Al.slice(1);
    Ln(Wy, "on" + Vy)
}
Ln(Mp, "onAnimationEnd");
Ln(Ip, "onAnimationIteration");
Ln(Dp, "onAnimationStart");
Ln("dblclick", "onDoubleClick");
Ln("focusin", "onFocus");
Ln("focusout", "onBlur");
Ln(zp, "onTransitionEnd");
Ur("onMouseEnter", ["mouseout", "mouseover"]);
Ur("onMouseLeave", ["mouseout", "mouseover"]);
Ur("onPointerEnter", ["pointerout", "pointerover"]);
Ur("onPointerLeave", ["pointerout", "pointerover"]);
rr("onChange", "change click focusin focusout input keydown keyup selectionchange".split(" "));
rr("onSelect", "focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" "));
rr("onBeforeInput", ["compositionend", "keypress", "textInput", "paste"]);
rr("onCompositionEnd", "compositionend focusout keydown keypress keyup mousedown".split(" "));
rr("onCompositionStart", "compositionstart focusout keydown keypress keyup mousedown".split(" "));
rr("onCompositionUpdate", "compositionupdate focusout keydown keypress keyup mousedown".split(" "));
var So = "abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" ")
  , Hy = new Set("cancel close invalid load scroll toggle".split(" ").concat(So));
function sd(e, t, n) {
    var r = e.type || "unknown-event";
    e.currentTarget = n,
    Wg(r, t, void 0, e),
    e.currentTarget = null
}
function $p(e, t) {
    t = (t & 4) !== 0;
    for (var n = 0; n < e.length; n++) {
        var r = e[n]
          , o = r.event;
        r = r.listeners;
        e: {
            var i = void 0;
            if (t)
                for (var s = r.length - 1; 0 <= s; s--) {
                    var l = r[s]
                      , a = l.instance
                      , u = l.currentTarget;
                    if (l = l.listener,
                    a !== i && o.isPropagationStopped())
                        break e;
                    sd(o, l, u),
                    i = a
                }
            else
                for (s = 0; s < r.length; s++) {
                    if (l = r[s],
                    a = l.instance,
                    u = l.currentTarget,
                    l = l.listener,
                    a !== i && o.isPropagationStopped())
                        break e;
                    sd(o, l, u),
                    i = a
                }
        }
    }
    if (is)
        throw e = fa,
        is = !1,
        fa = null,
        e
}
function oe(e, t) {
    var n = t[Sa];
    n === void 0 && (n = t[Sa] = new Set);
    var r = e + "__bubble";
    n.has(r) || (Up(t, e, 2, !1),
    n.add(r))
}
function jl(e, t, n) {
    var r = 0;
    t && (r |= 4),
    Up(n, e, r, t)
}
var Ti = "_reactListening" + Math.random().toString(36).slice(2);
function Uo(e) {
    if (!e[Ti]) {
        e[Ti] = !0,
        Gf.forEach(function(n) {
            n !== "selectionchange" && (Hy.has(n) || jl(n, !1, e),
            jl(n, !0, e))
        });
        var t = e.nodeType === 9 ? e : e.ownerDocument;
        t === null || t[Ti] || (t[Ti] = !0,
        jl("selectionchange", !1, t))
    }
}
function Up(e, t, n, r) {
    switch (bp(t)) {
    case 1:
        var o = iy;
        break;
    case 4:
        o = sy;
        break;
    default:
        o = Eu
    }
    n = o.bind(null, t, n, e),
    o = void 0,
    !da || t !== "touchstart" && t !== "touchmove" && t !== "wheel" || (o = !0),
    r ? o !== void 0 ? e.addEventListener(t, n, {
        capture: !0,
        passive: o
    }) : e.addEventListener(t, n, !0) : o !== void 0 ? e.addEventListener(t, n, {
        passive: o
    }) : e.addEventListener(t, n, !1)
}
function Ol(e, t, n, r, o) {
    var i = r;
    if (!(t & 1) && !(t & 2) && r !== null)
        e: for (; ; ) {
            if (r === null)
                return;
            var s = r.tag;
            if (s === 3 || s === 4) {
                var l = r.stateNode.containerInfo;
                if (l === o || l.nodeType === 8 && l.parentNode === o)
                    break;
                if (s === 4)
                    for (s = r.return; s !== null; ) {
                        var a = s.tag;
                        if ((a === 3 || a === 4) && (a = s.stateNode.containerInfo,
                        a === o || a.nodeType === 8 && a.parentNode === o))
                            return;
                        s = s.return
                    }
                for (; l !== null; ) {
                    if (s = Bn(l),
                    s === null)
                        return;
                    if (a = s.tag,
                    a === 5 || a === 6) {
                        r = i = s;
                        continue e
                    }
                    l = l.parentNode
                }
            }
            r = r.return
        }
    cp(function() {
        var u = i
          , d = gu(n)
          , f = [];
        e: {
            var c = Fp.get(e);
            if (c !== void 0) {
                var y = Cu
                  , w = e;
                switch (e) {
                case "keypress":
                    if (Qi(n) === 0)
                        break e;
                case "keydown":
                case "keyup":
                    y = Ey;
                    break;
                case "focusin":
                    w = "focus",
                    y = kl;
                    break;
                case "focusout":
                    w = "blur",
                    y = kl;
                    break;
                case "beforeblur":
                case "afterblur":
                    y = kl;
                    break;
                case "click":
                    if (n.button === 2)
                        break e;
                case "auxclick":
                case "dblclick":
                case "mousedown":
                case "mousemove":
                case "mouseup":
                case "mouseout":
                case "mouseover":
                case "contextmenu":
                    y = Gc;
                    break;
                case "drag":
                case "dragend":
                case "dragenter":
                case "dragexit":
                case "dragleave":
                case "dragover":
                case "dragstart":
                case "drop":
                    y = uy;
                    break;
                case "touchcancel":
                case "touchend":
                case "touchmove":
                case "touchstart":
                    y = by;
                    break;
                case Mp:
                case Ip:
                case Dp:
                    y = fy;
                    break;
                case zp:
                    y = Py;
                    break;
                case "scroll":
                    y = ly;
                    break;
                case "wheel":
                    y = Ty;
                    break;
                case "copy":
                case "cut":
                case "paste":
                    y = hy;
                    break;
                case "gotpointercapture":
                case "lostpointercapture":
                case "pointercancel":
                case "pointerdown":
                case "pointermove":
                case "pointerout":
                case "pointerover":
                case "pointerup":
                    y = Xc
                }
                var x = (t & 4) !== 0
                  , E = !x && e === "scroll"
                  , h = x ? c !== null ? c + "Capture" : null : c;
                x = [];
                for (var p = u, v; p !== null; ) {
                    v = p;
                    var S = v.stateNode;
                    if (v.tag === 5 && S !== null && (v = S,
                    h !== null && (S = Mo(p, h),
                    S != null && x.push(Bo(p, S, v)))),
                    E)
                        break;
                    p = p.return
                }
                0 < x.length && (c = new y(c,w,null,n,d),
                f.push({
                    event: c,
                    listeners: x
                }))
            }
        }
        if (!(t & 7)) {
            e: {
                if (c = e === "mouseover" || e === "pointerover",
                y = e === "mouseout" || e === "pointerout",
                c && n !== ua && (w = n.relatedTarget || n.fromElement) && (Bn(w) || w[Vt]))
                    break e;
                if ((y || c) && (c = d.window === d ? d : (c = d.ownerDocument) ? c.defaultView || c.parentWindow : window,
                y ? (w = n.relatedTarget || n.toElement,
                y = u,
                w = w ? Bn(w) : null,
                w !== null && (E = or(w),
                w !== E || w.tag !== 5 && w.tag !== 6) && (w = null)) : (y = null,
                w = u),
                y !== w)) {
                    if (x = Gc,
                    S = "onMouseLeave",
                    h = "onMouseEnter",
                    p = "mouse",
                    (e === "pointerout" || e === "pointerover") && (x = Xc,
                    S = "onPointerLeave",
                    h = "onPointerEnter",
                    p = "pointer"),
                    E = y == null ? c : gr(y),
                    v = w == null ? c : gr(w),
                    c = new x(S,p + "leave",y,n,d),
                    c.target = E,
                    c.relatedTarget = v,
                    S = null,
                    Bn(d) === u && (x = new x(h,p + "enter",w,n,d),
                    x.target = v,
                    x.relatedTarget = E,
                    S = x),
                    E = S,
                    y && w)
                        t: {
                            for (x = y,
                            h = w,
                            p = 0,
                            v = x; v; v = dr(v))
                                p++;
                            for (v = 0,
                            S = h; S; S = dr(S))
                                v++;
                            for (; 0 < p - v; )
                                x = dr(x),
                                p--;
                            for (; 0 < v - p; )
                                h = dr(h),
                                v--;
                            for (; p--; ) {
                                if (x === h || h !== null && x === h.alternate)
                                    break t;
                                x = dr(x),
                                h = dr(h)
                            }
                            x = null
                        }
                    else
                        x = null;
                    y !== null && ld(f, c, y, x, !1),
                    w !== null && E !== null && ld(f, E, w, x, !0)
                }
            }
            e: {
                if (c = u ? gr(u) : window,
                y = c.nodeName && c.nodeName.toLowerCase(),
                y === "select" || y === "input" && c.type === "file")
                    var C = My;
                else if (Jc(c))
                    if (Ap)
                        C = Fy;
                    else {
                        C = Dy;
                        var P = Iy
                    }
                else
                    (y = c.nodeName) && y.toLowerCase() === "input" && (c.type === "checkbox" || c.type === "radio") && (C = zy);
                if (C && (C = C(e, u))) {
                    Rp(f, C, n, d);
                    break e
                }
                P && P(e, c, u),
                e === "focusout" && (P = c._wrapperState) && P.controlled && c.type === "number" && oa(c, "number", c.value)
            }
            switch (P = u ? gr(u) : window,
            e) {
            case "focusin":
                (Jc(P) || P.contentEditable === "true") && (mr = P,
                va = u,
                No = null);
                break;
            case "focusout":
                No = va = mr = null;
                break;
            case "mousedown":
                ga = !0;
                break;
            case "contextmenu":
            case "mouseup":
            case "dragend":
                ga = !1,
                od(f, n, d);
                break;
            case "selectionchange":
                if (By)
                    break;
            case "keydown":
            case "keyup":
                od(f, n, d)
            }
            var b;
            if (ku)
                e: {
                    switch (e) {
                    case "compositionstart":
                        var N = "onCompositionStart";
                        break e;
                    case "compositionend":
                        N = "onCompositionEnd";
                        break e;
                    case "compositionupdate":
                        N = "onCompositionUpdate";
                        break e
                    }
                    N = void 0
                }
            else
                hr ? Np(e, n) && (N = "onCompositionEnd") : e === "keydown" && n.keyCode === 229 && (N = "onCompositionStart");
            N && (Pp && n.locale !== "ko" && (hr || N !== "onCompositionStart" ? N === "onCompositionEnd" && hr && (b = kp()) : (vn = d,
            Su = "value"in vn ? vn.value : vn.textContent,
            hr = !0)),
            P = cs(u, N),
            0 < P.length && (N = new Yc(N,e,null,n,d),
            f.push({
                event: N,
                listeners: P
            }),
            b ? N.data = b : (b = Tp(n),
            b !== null && (N.data = b)))),
            (b = Ay ? jy(e, n) : Oy(e, n)) && (u = cs(u, "onBeforeInput"),
            0 < u.length && (d = new Yc("onBeforeInput","beforeinput",null,n,d),
            f.push({
                event: d,
                listeners: u
            }),
            d.data = b))
        }
        $p(f, t)
    })
}
function Bo(e, t, n) {
    return {
        instance: e,
        listener: t,
        currentTarget: n
    }
}
function cs(e, t) {
    for (var n = t + "Capture", r = []; e !== null; ) {
        var o = e
          , i = o.stateNode;
        o.tag === 5 && i !== null && (o = i,
        i = Mo(e, n),
        i != null && r.unshift(Bo(e, i, o)),
        i = Mo(e, t),
        i != null && r.push(Bo(e, i, o))),
        e = e.return
    }
    return r
}
function dr(e) {
    if (e === null)
        return null;
    do
        e = e.return;
    while (e && e.tag !== 5);
    return e || null
}
function ld(e, t, n, r, o) {
    for (var i = t._reactName, s = []; n !== null && n !== r; ) {
        var l = n
          , a = l.alternate
          , u = l.stateNode;
        if (a !== null && a === r)
            break;
        l.tag === 5 && u !== null && (l = u,
        o ? (a = Mo(n, i),
        a != null && s.unshift(Bo(n, a, l))) : o || (a = Mo(n, i),
        a != null && s.push(Bo(n, a, l)))),
        n = n.return
    }
    s.length !== 0 && e.push({
        event: t,
        listeners: s
    })
}
var Qy = /\r\n?/g
  , Ky = /\u0000|\uFFFD/g;
function ad(e) {
    return (typeof e == "string" ? e : "" + e).replace(Qy, `
`).replace(Ky, "")
}
function Ri(e, t, n) {
    if (t = ad(t),
    ad(e) !== t && n)
        throw Error(A(425))
}
function ds() {}
var ya = null
  , xa = null;
function wa(e, t) {
    return e === "textarea" || e === "noscript" || typeof t.children == "string" || typeof t.children == "number" || typeof t.dangerouslySetInnerHTML == "object" && t.dangerouslySetInnerHTML !== null && t.dangerouslySetInnerHTML.__html != null
}
var Ea = typeof setTimeout == "function" ? setTimeout : void 0
  , Gy = typeof clearTimeout == "function" ? clearTimeout : void 0
  , ud = typeof Promise == "function" ? Promise : void 0
  , Yy = typeof queueMicrotask == "function" ? queueMicrotask : typeof ud < "u" ? function(e) {
    return ud.resolve(null).then(e).catch(Xy)
}
: Ea;
function Xy(e) {
    setTimeout(function() {
        throw e
    })
}
function _l(e, t) {
    var n = t
      , r = 0;
    do {
        var o = n.nextSibling;
        if (e.removeChild(n),
        o && o.nodeType === 8)
            if (n = o.data,
            n === "/$") {
                if (r === 0) {
                    e.removeChild(o),
                    zo(t);
                    return
                }
                r--
            } else
                n !== "$" && n !== "$?" && n !== "$!" || r++;
        n = o
    } while (n);
    zo(t)
}
function Sn(e) {
    for (; e != null; e = e.nextSibling) {
        var t = e.nodeType;
        if (t === 1 || t === 3)
            break;
        if (t === 8) {
            if (t = e.data,
            t === "$" || t === "$!" || t === "$?")
                break;
            if (t === "/$")
                return null
        }
    }
    return e
}
function cd(e) {
    e = e.previousSibling;
    for (var t = 0; e; ) {
        if (e.nodeType === 8) {
            var n = e.data;
            if (n === "$" || n === "$!" || n === "$?") {
                if (t === 0)
                    return e;
                t--
            } else
                n === "/$" && t++
        }
        e = e.previousSibling
    }
    return null
}
var Zr = Math.random().toString(36).slice(2)
  , jt = "__reactFiber$" + Zr
  , Wo = "__reactProps$" + Zr
  , Vt = "__reactContainer$" + Zr
  , Sa = "__reactEvents$" + Zr
  , qy = "__reactListeners$" + Zr
  , Zy = "__reactHandles$" + Zr;
function Bn(e) {
    var t = e[jt];
    if (t)
        return t;
    for (var n = e.parentNode; n; ) {
        if (t = n[Vt] || n[jt]) {
            if (n = t.alternate,
            t.child !== null || n !== null && n.child !== null)
                for (e = cd(e); e !== null; ) {
                    if (n = e[jt])
                        return n;
                    e = cd(e)
                }
            return t
        }
        e = n,
        n = e.parentNode
    }
    return null
}
function ai(e) {
    return e = e[jt] || e[Vt],
    !e || e.tag !== 5 && e.tag !== 6 && e.tag !== 13 && e.tag !== 3 ? null : e
}
function gr(e) {
    if (e.tag === 5 || e.tag === 6)
        return e.stateNode;
    throw Error(A(33))
}
function $s(e) {
    return e[Wo] || null
}
var Ca = []
  , yr = -1;
function Mn(e) {
    return {
        current: e
    }
}
function ie(e) {
    0 > yr || (e.current = Ca[yr],
    Ca[yr] = null,
    yr--)
}
function te(e, t) {
    yr++,
    Ca[yr] = e.current,
    e.current = t
}
var Rn = {}
  , je = Mn(Rn)
  , Ue = Mn(!1)
  , qn = Rn;
function Br(e, t) {
    var n = e.type.contextTypes;
    if (!n)
        return Rn;
    var r = e.stateNode;
    if (r && r.__reactInternalMemoizedUnmaskedChildContext === t)
        return r.__reactInternalMemoizedMaskedChildContext;
    var o = {}, i;
    for (i in n)
        o[i] = t[i];
    return r && (e = e.stateNode,
    e.__reactInternalMemoizedUnmaskedChildContext = t,
    e.__reactInternalMemoizedMaskedChildContext = o),
    o
}
function Be(e) {
    return e = e.childContextTypes,
    e != null
}
function fs() {
    ie(Ue),
    ie(je)
}
function dd(e, t, n) {
    if (je.current !== Rn)
        throw Error(A(168));
    te(je, t),
    te(Ue, n)
}
function Bp(e, t, n) {
    var r = e.stateNode;
    if (t = t.childContextTypes,
    typeof r.getChildContext != "function")
        return n;
    r = r.getChildContext();
    for (var o in r)
        if (!(o in t))
            throw Error(A(108, Ig(e) || "Unknown", o));
    return de({}, n, r)
}
function ps(e) {
    return e = (e = e.stateNode) && e.__reactInternalMemoizedMergedChildContext || Rn,
    qn = je.current,
    te(je, e),
    te(Ue, Ue.current),
    !0
}
function fd(e, t, n) {
    var r = e.stateNode;
    if (!r)
        throw Error(A(169));
    n ? (e = Bp(e, t, qn),
    r.__reactInternalMemoizedMergedChildContext = e,
    ie(Ue),
    ie(je),
    te(je, e)) : ie(Ue),
    te(Ue, n)
}
var Ft = null
  , Us = !1
  , Ll = !1;
function Wp(e) {
    Ft === null ? Ft = [e] : Ft.push(e)
}
function Jy(e) {
    Us = !0,
    Wp(e)
}
function In() {
    if (!Ll && Ft !== null) {
        Ll = !0;
        var e = 0
          , t = J;
        try {
            var n = Ft;
            for (J = 1; e < n.length; e++) {
                var r = n[e];
                do
                    r = r(!0);
                while (r !== null)
            }
            Ft = null,
            Us = !1
        } catch (o) {
            throw Ft !== null && (Ft = Ft.slice(e + 1)),
            hp(yu, In),
            o
        } finally {
            J = t,
            Ll = !1
        }
    }
    return null
}
var xr = []
  , wr = 0
  , hs = null
  , ms = 0
  , nt = []
  , rt = 0
  , Zn = null
  , $t = 1
  , Ut = "";
function zn(e, t) {
    xr[wr++] = ms,
    xr[wr++] = hs,
    hs = e,
    ms = t
}
function Vp(e, t, n) {
    nt[rt++] = $t,
    nt[rt++] = Ut,
    nt[rt++] = Zn,
    Zn = e;
    var r = $t;
    e = Ut;
    var o = 32 - vt(r) - 1;
    r &= ~(1 << o),
    n += 1;
    var i = 32 - vt(t) + o;
    if (30 < i) {
        var s = o - o % 5;
        i = (r & (1 << s) - 1).toString(32),
        r >>= s,
        o -= s,
        $t = 1 << 32 - vt(t) + o | n << o | r,
        Ut = i + e
    } else
        $t = 1 << i | n << o | r,
        Ut = e
}
function Nu(e) {
    e.return !== null && (zn(e, 1),
    Vp(e, 1, 0))
}
function Tu(e) {
    for (; e === hs; )
        hs = xr[--wr],
        xr[wr] = null,
        ms = xr[--wr],
        xr[wr] = null;
    for (; e === Zn; )
        Zn = nt[--rt],
        nt[rt] = null,
        Ut = nt[--rt],
        nt[rt] = null,
        $t = nt[--rt],
        nt[rt] = null
}
var Xe = null
  , Ye = null
  , se = !1
  , mt = null;
function Hp(e, t) {
    var n = ot(5, null, null, 0);
    n.elementType = "DELETED",
    n.stateNode = t,
    n.return = e,
    t = e.deletions,
    t === null ? (e.deletions = [n],
    e.flags |= 16) : t.push(n)
}
function pd(e, t) {
    switch (e.tag) {
    case 5:
        var n = e.type;
        return t = t.nodeType !== 1 || n.toLowerCase() !== t.nodeName.toLowerCase() ? null : t,
        t !== null ? (e.stateNode = t,
        Xe = e,
        Ye = Sn(t.firstChild),
        !0) : !1;
    case 6:
        return t = e.pendingProps === "" || t.nodeType !== 3 ? null : t,
        t !== null ? (e.stateNode = t,
        Xe = e,
        Ye = null,
        !0) : !1;
    case 13:
        return t = t.nodeType !== 8 ? null : t,
        t !== null ? (n = Zn !== null ? {
            id: $t,
            overflow: Ut
        } : null,
        e.memoizedState = {
            dehydrated: t,
            treeContext: n,
            retryLane: 1073741824
        },
        n = ot(18, null, null, 0),
        n.stateNode = t,
        n.return = e,
        e.child = n,
        Xe = e,
        Ye = null,
        !0) : !1;
    default:
        return !1
    }
}
function ba(e) {
    return (e.mode & 1) !== 0 && (e.flags & 128) === 0
}
function ka(e) {
    if (se) {
        var t = Ye;
        if (t) {
            var n = t;
            if (!pd(e, t)) {
                if (ba(e))
                    throw Error(A(418));
                t = Sn(n.nextSibling);
                var r = Xe;
                t && pd(e, t) ? Hp(r, n) : (e.flags = e.flags & -4097 | 2,
                se = !1,
                Xe = e)
            }
        } else {
            if (ba(e))
                throw Error(A(418));
            e.flags = e.flags & -4097 | 2,
            se = !1,
            Xe = e
        }
    }
}
function hd(e) {
    for (e = e.return; e !== null && e.tag !== 5 && e.tag !== 3 && e.tag !== 13; )
        e = e.return;
    Xe = e
}
function Ai(e) {
    if (e !== Xe)
        return !1;
    if (!se)
        return hd(e),
        se = !0,
        !1;
    var t;
    if ((t = e.tag !== 3) && !(t = e.tag !== 5) && (t = e.type,
    t = t !== "head" && t !== "body" && !wa(e.type, e.memoizedProps)),
    t && (t = Ye)) {
        if (ba(e))
            throw Qp(),
            Error(A(418));
        for (; t; )
            Hp(e, t),
            t = Sn(t.nextSibling)
    }
    if (hd(e),
    e.tag === 13) {
        if (e = e.memoizedState,
        e = e !== null ? e.dehydrated : null,
        !e)
            throw Error(A(317));
        e: {
            for (e = e.nextSibling,
            t = 0; e; ) {
                if (e.nodeType === 8) {
                    var n = e.data;
                    if (n === "/$") {
                        if (t === 0) {
                            Ye = Sn(e.nextSibling);
                            break e
                        }
                        t--
                    } else
                        n !== "$" && n !== "$!" && n !== "$?" || t++
                }
                e = e.nextSibling
            }
            Ye = null
        }
    } else
        Ye = Xe ? Sn(e.stateNode.nextSibling) : null;
    return !0
}
function Qp() {
    for (var e = Ye; e; )
        e = Sn(e.nextSibling)
}
function Wr() {
    Ye = Xe = null,
    se = !1
}
function Ru(e) {
    mt === null ? mt = [e] : mt.push(e)
}
var e0 = Yt.ReactCurrentBatchConfig;
function po(e, t, n) {
    if (e = n.ref,
    e !== null && typeof e != "function" && typeof e != "object") {
        if (n._owner) {
            if (n = n._owner,
            n) {
                if (n.tag !== 1)
                    throw Error(A(309));
                var r = n.stateNode
            }
            if (!r)
                throw Error(A(147, e));
            var o = r
              , i = "" + e;
            return t !== null && t.ref !== null && typeof t.ref == "function" && t.ref._stringRef === i ? t.ref : (t = function(s) {
                var l = o.refs;
                s === null ? delete l[i] : l[i] = s
            }
            ,
            t._stringRef = i,
            t)
        }
        if (typeof e != "string")
            throw Error(A(284));
        if (!n._owner)
            throw Error(A(290, e))
    }
    return e
}
function ji(e, t) {
    throw e = Object.prototype.toString.call(t),
    Error(A(31, e === "[object Object]" ? "object with keys {" + Object.keys(t).join(", ") + "}" : e))
}
function md(e) {
    var t = e._init;
    return t(e._payload)
}
function Kp(e) {
    function t(h, p) {
        if (e) {
            var v = h.deletions;
            v === null ? (h.deletions = [p],
            h.flags |= 16) : v.push(p)
        }
    }
    function n(h, p) {
        if (!e)
            return null;
        for (; p !== null; )
            t(h, p),
            p = p.sibling;
        return null
    }
    function r(h, p) {
        for (h = new Map; p !== null; )
            p.key !== null ? h.set(p.key, p) : h.set(p.index, p),
            p = p.sibling;
        return h
    }
    function o(h, p) {
        return h = Pn(h, p),
        h.index = 0,
        h.sibling = null,
        h
    }
    function i(h, p, v) {
        return h.index = v,
        e ? (v = h.alternate,
        v !== null ? (v = v.index,
        v < p ? (h.flags |= 2,
        p) : v) : (h.flags |= 2,
        p)) : (h.flags |= 1048576,
        p)
    }
    function s(h) {
        return e && h.alternate === null && (h.flags |= 2),
        h
    }
    function l(h, p, v, S) {
        return p === null || p.tag !== 6 ? (p = Ul(v, h.mode, S),
        p.return = h,
        p) : (p = o(p, v),
        p.return = h,
        p)
    }
    function a(h, p, v, S) {
        var C = v.type;
        return C === pr ? d(h, p, v.props.children, S, v.key) : p !== null && (p.elementType === C || typeof C == "object" && C !== null && C.$$typeof === ln && md(C) === p.type) ? (S = o(p, v.props),
        S.ref = po(h, p, v),
        S.return = h,
        S) : (S = Ji(v.type, v.key, v.props, null, h.mode, S),
        S.ref = po(h, p, v),
        S.return = h,
        S)
    }
    function u(h, p, v, S) {
        return p === null || p.tag !== 4 || p.stateNode.containerInfo !== v.containerInfo || p.stateNode.implementation !== v.implementation ? (p = Bl(v, h.mode, S),
        p.return = h,
        p) : (p = o(p, v.children || []),
        p.return = h,
        p)
    }
    function d(h, p, v, S, C) {
        return p === null || p.tag !== 7 ? (p = Xn(v, h.mode, S, C),
        p.return = h,
        p) : (p = o(p, v),
        p.return = h,
        p)
    }
    function f(h, p, v) {
        if (typeof p == "string" && p !== "" || typeof p == "number")
            return p = Ul("" + p, h.mode, v),
            p.return = h,
            p;
        if (typeof p == "object" && p !== null) {
            switch (p.$$typeof) {
            case wi:
                return v = Ji(p.type, p.key, p.props, null, h.mode, v),
                v.ref = po(h, null, p),
                v.return = h,
                v;
            case fr:
                return p = Bl(p, h.mode, v),
                p.return = h,
                p;
            case ln:
                var S = p._init;
                return f(h, S(p._payload), v)
            }
            if (wo(p) || lo(p))
                return p = Xn(p, h.mode, v, null),
                p.return = h,
                p;
            ji(h, p)
        }
        return null
    }
    function c(h, p, v, S) {
        var C = p !== null ? p.key : null;
        if (typeof v == "string" && v !== "" || typeof v == "number")
            return C !== null ? null : l(h, p, "" + v, S);
        if (typeof v == "object" && v !== null) {
            switch (v.$$typeof) {
            case wi:
                return v.key === C ? a(h, p, v, S) : null;
            case fr:
                return v.key === C ? u(h, p, v, S) : null;
            case ln:
                return C = v._init,
                c(h, p, C(v._payload), S)
            }
            if (wo(v) || lo(v))
                return C !== null ? null : d(h, p, v, S, null);
            ji(h, v)
        }
        return null
    }
    function y(h, p, v, S, C) {
        if (typeof S == "string" && S !== "" || typeof S == "number")
            return h = h.get(v) || null,
            l(p, h, "" + S, C);
        if (typeof S == "object" && S !== null) {
            switch (S.$$typeof) {
            case wi:
                return h = h.get(S.key === null ? v : S.key) || null,
                a(p, h, S, C);
            case fr:
                return h = h.get(S.key === null ? v : S.key) || null,
                u(p, h, S, C);
            case ln:
                var P = S._init;
                return y(h, p, v, P(S._payload), C)
            }
            if (wo(S) || lo(S))
                return h = h.get(v) || null,
                d(p, h, S, C, null);
            ji(p, S)
        }
        return null
    }
    function w(h, p, v, S) {
        for (var C = null, P = null, b = p, N = p = 0, _ = null; b !== null && N < v.length; N++) {
            b.index > N ? (_ = b,
            b = null) : _ = b.sibling;
            var O = c(h, b, v[N], S);
            if (O === null) {
                b === null && (b = _);
                break
            }
            e && b && O.alternate === null && t(h, b),
            p = i(O, p, N),
            P === null ? C = O : P.sibling = O,
            P = O,
            b = _
        }
        if (N === v.length)
            return n(h, b),
            se && zn(h, N),
            C;
        if (b === null) {
            for (; N < v.length; N++)
                b = f(h, v[N], S),
                b !== null && (p = i(b, p, N),
                P === null ? C = b : P.sibling = b,
                P = b);
            return se && zn(h, N),
            C
        }
        for (b = r(h, b); N < v.length; N++)
            _ = y(b, h, N, v[N], S),
            _ !== null && (e && _.alternate !== null && b.delete(_.key === null ? N : _.key),
            p = i(_, p, N),
            P === null ? C = _ : P.sibling = _,
            P = _);
        return e && b.forEach(function($) {
            return t(h, $)
        }),
        se && zn(h, N),
        C
    }
    function x(h, p, v, S) {
        var C = lo(v);
        if (typeof C != "function")
            throw Error(A(150));
        if (v = C.call(v),
        v == null)
            throw Error(A(151));
        for (var P = C = null, b = p, N = p = 0, _ = null, O = v.next(); b !== null && !O.done; N++,
        O = v.next()) {
            b.index > N ? (_ = b,
            b = null) : _ = b.sibling;
            var $ = c(h, b, O.value, S);
            if ($ === null) {
                b === null && (b = _);
                break
            }
            e && b && $.alternate === null && t(h, b),
            p = i($, p, N),
            P === null ? C = $ : P.sibling = $,
            P = $,
            b = _
        }
        if (O.done)
            return n(h, b),
            se && zn(h, N),
            C;
        if (b === null) {
            for (; !O.done; N++,
            O = v.next())
                O = f(h, O.value, S),
                O !== null && (p = i(O, p, N),
                P === null ? C = O : P.sibling = O,
                P = O);
            return se && zn(h, N),
            C
        }
        for (b = r(h, b); !O.done; N++,
        O = v.next())
            O = y(b, h, N, O.value, S),
            O !== null && (e && O.alternate !== null && b.delete(O.key === null ? N : O.key),
            p = i(O, p, N),
            P === null ? C = O : P.sibling = O,
            P = O);
        return e && b.forEach(function(D) {
            return t(h, D)
        }),
        se && zn(h, N),
        C
    }
    function E(h, p, v, S) {
        if (typeof v == "object" && v !== null && v.type === pr && v.key === null && (v = v.props.children),
        typeof v == "object" && v !== null) {
            switch (v.$$typeof) {
            case wi:
                e: {
                    for (var C = v.key, P = p; P !== null; ) {
                        if (P.key === C) {
                            if (C = v.type,
                            C === pr) {
                                if (P.tag === 7) {
                                    n(h, P.sibling),
                                    p = o(P, v.props.children),
                                    p.return = h,
                                    h = p;
                                    break e
                                }
                            } else if (P.elementType === C || typeof C == "object" && C !== null && C.$$typeof === ln && md(C) === P.type) {
                                n(h, P.sibling),
                                p = o(P, v.props),
                                p.ref = po(h, P, v),
                                p.return = h,
                                h = p;
                                break e
                            }
                            n(h, P);
                            break
                        } else
                            t(h, P);
                        P = P.sibling
                    }
                    v.type === pr ? (p = Xn(v.props.children, h.mode, S, v.key),
                    p.return = h,
                    h = p) : (S = Ji(v.type, v.key, v.props, null, h.mode, S),
                    S.ref = po(h, p, v),
                    S.return = h,
                    h = S)
                }
                return s(h);
            case fr:
                e: {
                    for (P = v.key; p !== null; ) {
                        if (p.key === P)
                            if (p.tag === 4 && p.stateNode.containerInfo === v.containerInfo && p.stateNode.implementation === v.implementation) {
                                n(h, p.sibling),
                                p = o(p, v.children || []),
                                p.return = h,
                                h = p;
                                break e
                            } else {
                                n(h, p);
                                break
                            }
                        else
                            t(h, p);
                        p = p.sibling
                    }
                    p = Bl(v, h.mode, S),
                    p.return = h,
                    h = p
                }
                return s(h);
            case ln:
                return P = v._init,
                E(h, p, P(v._payload), S)
            }
            if (wo(v))
                return w(h, p, v, S);
            if (lo(v))
                return x(h, p, v, S);
            ji(h, v)
        }
        return typeof v == "string" && v !== "" || typeof v == "number" ? (v = "" + v,
        p !== null && p.tag === 6 ? (n(h, p.sibling),
        p = o(p, v),
        p.return = h,
        h = p) : (n(h, p),
        p = Ul(v, h.mode, S),
        p.return = h,
        h = p),
        s(h)) : n(h, p)
    }
    return E
}
var Vr = Kp(!0)
  , Gp = Kp(!1)
  , vs = Mn(null)
  , gs = null
  , Er = null
  , Au = null;
function ju() {
    Au = Er = gs = null
}
function Ou(e) {
    var t = vs.current;
    ie(vs),
    e._currentValue = t
}
function Pa(e, t, n) {
    for (; e !== null; ) {
        var r = e.alternate;
        if ((e.childLanes & t) !== t ? (e.childLanes |= t,
        r !== null && (r.childLanes |= t)) : r !== null && (r.childLanes & t) !== t && (r.childLanes |= t),
        e === n)
            break;
        e = e.return
    }
}
function Tr(e, t) {
    gs = e,
    Au = Er = null,
    e = e.dependencies,
    e !== null && e.firstContext !== null && (e.lanes & t && ($e = !0),
    e.firstContext = null)
}
function st(e) {
    var t = e._currentValue;
    if (Au !== e)
        if (e = {
            context: e,
            memoizedValue: t,
            next: null
        },
        Er === null) {
            if (gs === null)
                throw Error(A(308));
            Er = e,
            gs.dependencies = {
                lanes: 0,
                firstContext: e
            }
        } else
            Er = Er.next = e;
    return t
}
var Wn = null;
function _u(e) {
    Wn === null ? Wn = [e] : Wn.push(e)
}
function Yp(e, t, n, r) {
    var o = t.interleaved;
    return o === null ? (n.next = n,
    _u(t)) : (n.next = o.next,
    o.next = n),
    t.interleaved = n,
    Ht(e, r)
}
function Ht(e, t) {
    e.lanes |= t;
    var n = e.alternate;
    for (n !== null && (n.lanes |= t),
    n = e,
    e = e.return; e !== null; )
        e.childLanes |= t,
        n = e.alternate,
        n !== null && (n.childLanes |= t),
        n = e,
        e = e.return;
    return n.tag === 3 ? n.stateNode : null
}
var an = !1;
function Lu(e) {
    e.updateQueue = {
        baseState: e.memoizedState,
        firstBaseUpdate: null,
        lastBaseUpdate: null,
        shared: {
            pending: null,
            interleaved: null,
            lanes: 0
        },
        effects: null
    }
}
function Xp(e, t) {
    e = e.updateQueue,
    t.updateQueue === e && (t.updateQueue = {
        baseState: e.baseState,
        firstBaseUpdate: e.firstBaseUpdate,
        lastBaseUpdate: e.lastBaseUpdate,
        shared: e.shared,
        effects: e.effects
    })
}
function Bt(e, t) {
    return {
        eventTime: e,
        lane: t,
        tag: 0,
        payload: null,
        callback: null,
        next: null
    }
}
function Cn(e, t, n) {
    var r = e.updateQueue;
    if (r === null)
        return null;
    if (r = r.shared,
    X & 2) {
        var o = r.pending;
        return o === null ? t.next = t : (t.next = o.next,
        o.next = t),
        r.pending = t,
        Ht(e, n)
    }
    return o = r.interleaved,
    o === null ? (t.next = t,
    _u(r)) : (t.next = o.next,
    o.next = t),
    r.interleaved = t,
    Ht(e, n)
}
function Ki(e, t, n) {
    if (t = t.updateQueue,
    t !== null && (t = t.shared,
    (n & 4194240) !== 0)) {
        var r = t.lanes;
        r &= e.pendingLanes,
        n |= r,
        t.lanes = n,
        xu(e, n)
    }
}
function vd(e, t) {
    var n = e.updateQueue
      , r = e.alternate;
    if (r !== null && (r = r.updateQueue,
    n === r)) {
        var o = null
          , i = null;
        if (n = n.firstBaseUpdate,
        n !== null) {
            do {
                var s = {
                    eventTime: n.eventTime,
                    lane: n.lane,
                    tag: n.tag,
                    payload: n.payload,
                    callback: n.callback,
                    next: null
                };
                i === null ? o = i = s : i = i.next = s,
                n = n.next
            } while (n !== null);
            i === null ? o = i = t : i = i.next = t
        } else
            o = i = t;
        n = {
            baseState: r.baseState,
            firstBaseUpdate: o,
            lastBaseUpdate: i,
            shared: r.shared,
            effects: r.effects
        },
        e.updateQueue = n;
        return
    }
    e = n.lastBaseUpdate,
    e === null ? n.firstBaseUpdate = t : e.next = t,
    n.lastBaseUpdate = t
}
function ys(e, t, n, r) {
    var o = e.updateQueue;
    an = !1;
    var i = o.firstBaseUpdate
      , s = o.lastBaseUpdate
      , l = o.shared.pending;
    if (l !== null) {
        o.shared.pending = null;
        var a = l
          , u = a.next;
        a.next = null,
        s === null ? i = u : s.next = u,
        s = a;
        var d = e.alternate;
        d !== null && (d = d.updateQueue,
        l = d.lastBaseUpdate,
        l !== s && (l === null ? d.firstBaseUpdate = u : l.next = u,
        d.lastBaseUpdate = a))
    }
    if (i !== null) {
        var f = o.baseState;
        s = 0,
        d = u = a = null,
        l = i;
        do {
            var c = l.lane
              , y = l.eventTime;
            if ((r & c) === c) {
                d !== null && (d = d.next = {
                    eventTime: y,
                    lane: 0,
                    tag: l.tag,
                    payload: l.payload,
                    callback: l.callback,
                    next: null
                });
                e: {
                    var w = e
                      , x = l;
                    switch (c = t,
                    y = n,
                    x.tag) {
                    case 1:
                        if (w = x.payload,
                        typeof w == "function") {
                            f = w.call(y, f, c);
                            break e
                        }
                        f = w;
                        break e;
                    case 3:
                        w.flags = w.flags & -65537 | 128;
                    case 0:
                        if (w = x.payload,
                        c = typeof w == "function" ? w.call(y, f, c) : w,
                        c == null)
                            break e;
                        f = de({}, f, c);
                        break e;
                    case 2:
                        an = !0
                    }
                }
                l.callback !== null && l.lane !== 0 && (e.flags |= 64,
                c = o.effects,
                c === null ? o.effects = [l] : c.push(l))
            } else
                y = {
                    eventTime: y,
                    lane: c,
                    tag: l.tag,
                    payload: l.payload,
                    callback: l.callback,
                    next: null
                },
                d === null ? (u = d = y,
                a = f) : d = d.next = y,
                s |= c;
            if (l = l.next,
            l === null) {
                if (l = o.shared.pending,
                l === null)
                    break;
                c = l,
                l = c.next,
                c.next = null,
                o.lastBaseUpdate = c,
                o.shared.pending = null
            }
        } while (!0);
        if (d === null && (a = f),
        o.baseState = a,
        o.firstBaseUpdate = u,
        o.lastBaseUpdate = d,
        t = o.shared.interleaved,
        t !== null) {
            o = t;
            do
                s |= o.lane,
                o = o.next;
            while (o !== t)
        } else
            i === null && (o.shared.lanes = 0);
        er |= s,
        e.lanes = s,
        e.memoizedState = f
    }
}
function gd(e, t, n) {
    if (e = t.effects,
    t.effects = null,
    e !== null)
        for (t = 0; t < e.length; t++) {
            var r = e[t]
              , o = r.callback;
            if (o !== null) {
                if (r.callback = null,
                r = n,
                typeof o != "function")
                    throw Error(A(191, o));
                o.call(r)
            }
        }
}
var ui = {}
  , _t = Mn(ui)
  , Vo = Mn(ui)
  , Ho = Mn(ui);
function Vn(e) {
    if (e === ui)
        throw Error(A(174));
    return e
}
function Mu(e, t) {
    switch (te(Ho, t),
    te(Vo, e),
    te(_t, ui),
    e = t.nodeType,
    e) {
    case 9:
    case 11:
        t = (t = t.documentElement) ? t.namespaceURI : sa(null, "");
        break;
    default:
        e = e === 8 ? t.parentNode : t,
        t = e.namespaceURI || null,
        e = e.tagName,
        t = sa(t, e)
    }
    ie(_t),
    te(_t, t)
}
function Hr() {
    ie(_t),
    ie(Vo),
    ie(Ho)
}
function qp(e) {
    Vn(Ho.current);
    var t = Vn(_t.current)
      , n = sa(t, e.type);
    t !== n && (te(Vo, e),
    te(_t, n))
}
function Iu(e) {
    Vo.current === e && (ie(_t),
    ie(Vo))
}
var ue = Mn(0);
function xs(e) {
    for (var t = e; t !== null; ) {
        if (t.tag === 13) {
            var n = t.memoizedState;
            if (n !== null && (n = n.dehydrated,
            n === null || n.data === "$?" || n.data === "$!"))
                return t
        } else if (t.tag === 19 && t.memoizedProps.revealOrder !== void 0) {
            if (t.flags & 128)
                return t
        } else if (t.child !== null) {
            t.child.return = t,
            t = t.child;
            continue
        }
        if (t === e)
            break;
        for (; t.sibling === null; ) {
            if (t.return === null || t.return === e)
                return null;
            t = t.return
        }
        t.sibling.return = t.return,
        t = t.sibling
    }
    return null
}
var Ml = [];
function Du() {
    for (var e = 0; e < Ml.length; e++)
        Ml[e]._workInProgressVersionPrimary = null;
    Ml.length = 0
}
var Gi = Yt.ReactCurrentDispatcher
  , Il = Yt.ReactCurrentBatchConfig
  , Jn = 0
  , ce = null
  , ye = null
  , we = null
  , ws = !1
  , To = !1
  , Qo = 0
  , t0 = 0;
function Ne() {
    throw Error(A(321))
}
function zu(e, t) {
    if (t === null)
        return !1;
    for (var n = 0; n < t.length && n < e.length; n++)
        if (!yt(e[n], t[n]))
            return !1;
    return !0
}
function Fu(e, t, n, r, o, i) {
    if (Jn = i,
    ce = t,
    t.memoizedState = null,
    t.updateQueue = null,
    t.lanes = 0,
    Gi.current = e === null || e.memoizedState === null ? i0 : s0,
    e = n(r, o),
    To) {
        i = 0;
        do {
            if (To = !1,
            Qo = 0,
            25 <= i)
                throw Error(A(301));
            i += 1,
            we = ye = null,
            t.updateQueue = null,
            Gi.current = l0,
            e = n(r, o)
        } while (To)
    }
    if (Gi.current = Es,
    t = ye !== null && ye.next !== null,
    Jn = 0,
    we = ye = ce = null,
    ws = !1,
    t)
        throw Error(A(300));
    return e
}
function $u() {
    var e = Qo !== 0;
    return Qo = 0,
    e
}
function Nt() {
    var e = {
        memoizedState: null,
        baseState: null,
        baseQueue: null,
        queue: null,
        next: null
    };
    return we === null ? ce.memoizedState = we = e : we = we.next = e,
    we
}
function lt() {
    if (ye === null) {
        var e = ce.alternate;
        e = e !== null ? e.memoizedState : null
    } else
        e = ye.next;
    var t = we === null ? ce.memoizedState : we.next;
    if (t !== null)
        we = t,
        ye = e;
    else {
        if (e === null)
            throw Error(A(310));
        ye = e,
        e = {
            memoizedState: ye.memoizedState,
            baseState: ye.baseState,
            baseQueue: ye.baseQueue,
            queue: ye.queue,
            next: null
        },
        we === null ? ce.memoizedState = we = e : we = we.next = e
    }
    return we
}
function Ko(e, t) {
    return typeof t == "function" ? t(e) : t
}
function Dl(e) {
    var t = lt()
      , n = t.queue;
    if (n === null)
        throw Error(A(311));
    n.lastRenderedReducer = e;
    var r = ye
      , o = r.baseQueue
      , i = n.pending;
    if (i !== null) {
        if (o !== null) {
            var s = o.next;
            o.next = i.next,
            i.next = s
        }
        r.baseQueue = o = i,
        n.pending = null
    }
    if (o !== null) {
        i = o.next,
        r = r.baseState;
        var l = s = null
          , a = null
          , u = i;
        do {
            var d = u.lane;
            if ((Jn & d) === d)
                a !== null && (a = a.next = {
                    lane: 0,
                    action: u.action,
                    hasEagerState: u.hasEagerState,
                    eagerState: u.eagerState,
                    next: null
                }),
                r = u.hasEagerState ? u.eagerState : e(r, u.action);
            else {
                var f = {
                    lane: d,
                    action: u.action,
                    hasEagerState: u.hasEagerState,
                    eagerState: u.eagerState,
                    next: null
                };
                a === null ? (l = a = f,
                s = r) : a = a.next = f,
                ce.lanes |= d,
                er |= d
            }
            u = u.next
        } while (u !== null && u !== i);
        a === null ? s = r : a.next = l,
        yt(r, t.memoizedState) || ($e = !0),
        t.memoizedState = r,
        t.baseState = s,
        t.baseQueue = a,
        n.lastRenderedState = r
    }
    if (e = n.interleaved,
    e !== null) {
        o = e;
        do
            i = o.lane,
            ce.lanes |= i,
            er |= i,
            o = o.next;
        while (o !== e)
    } else
        o === null && (n.lanes = 0);
    return [t.memoizedState, n.dispatch]
}
function zl(e) {
    var t = lt()
      , n = t.queue;
    if (n === null)
        throw Error(A(311));
    n.lastRenderedReducer = e;
    var r = n.dispatch
      , o = n.pending
      , i = t.memoizedState;
    if (o !== null) {
        n.pending = null;
        var s = o = o.next;
        do
            i = e(i, s.action),
            s = s.next;
        while (s !== o);
        yt(i, t.memoizedState) || ($e = !0),
        t.memoizedState = i,
        t.baseQueue === null && (t.baseState = i),
        n.lastRenderedState = i
    }
    return [i, r]
}
function Zp() {}
function Jp(e, t) {
    var n = ce
      , r = lt()
      , o = t()
      , i = !yt(r.memoizedState, o);
    if (i && (r.memoizedState = o,
    $e = !0),
    r = r.queue,
    Uu(nh.bind(null, n, r, e), [e]),
    r.getSnapshot !== t || i || we !== null && we.memoizedState.tag & 1) {
        if (n.flags |= 2048,
        Go(9, th.bind(null, n, r, o, t), void 0, null),
        Ee === null)
            throw Error(A(349));
        Jn & 30 || eh(n, t, o)
    }
    return o
}
function eh(e, t, n) {
    e.flags |= 16384,
    e = {
        getSnapshot: t,
        value: n
    },
    t = ce.updateQueue,
    t === null ? (t = {
        lastEffect: null,
        stores: null
    },
    ce.updateQueue = t,
    t.stores = [e]) : (n = t.stores,
    n === null ? t.stores = [e] : n.push(e))
}
function th(e, t, n, r) {
    t.value = n,
    t.getSnapshot = r,
    rh(t) && oh(e)
}
function nh(e, t, n) {
    return n(function() {
        rh(t) && oh(e)
    })
}
function rh(e) {
    var t = e.getSnapshot;
    e = e.value;
    try {
        var n = t();
        return !yt(e, n)
    } catch {
        return !0
    }
}
function oh(e) {
    var t = Ht(e, 1);
    t !== null && gt(t, e, 1, -1)
}
function yd(e) {
    var t = Nt();
    return typeof e == "function" && (e = e()),
    t.memoizedState = t.baseState = e,
    e = {
        pending: null,
        interleaved: null,
        lanes: 0,
        dispatch: null,
        lastRenderedReducer: Ko,
        lastRenderedState: e
    },
    t.queue = e,
    e = e.dispatch = o0.bind(null, ce, e),
    [t.memoizedState, e]
}
function Go(e, t, n, r) {
    return e = {
        tag: e,
        create: t,
        destroy: n,
        deps: r,
        next: null
    },
    t = ce.updateQueue,
    t === null ? (t = {
        lastEffect: null,
        stores: null
    },
    ce.updateQueue = t,
    t.lastEffect = e.next = e) : (n = t.lastEffect,
    n === null ? t.lastEffect = e.next = e : (r = n.next,
    n.next = e,
    e.next = r,
    t.lastEffect = e)),
    e
}
function ih() {
    return lt().memoizedState
}
function Yi(e, t, n, r) {
    var o = Nt();
    ce.flags |= e,
    o.memoizedState = Go(1 | t, n, void 0, r === void 0 ? null : r)
}
function Bs(e, t, n, r) {
    var o = lt();
    r = r === void 0 ? null : r;
    var i = void 0;
    if (ye !== null) {
        var s = ye.memoizedState;
        if (i = s.destroy,
        r !== null && zu(r, s.deps)) {
            o.memoizedState = Go(t, n, i, r);
            return
        }
    }
    ce.flags |= e,
    o.memoizedState = Go(1 | t, n, i, r)
}
function xd(e, t) {
    return Yi(8390656, 8, e, t)
}
function Uu(e, t) {
    return Bs(2048, 8, e, t)
}
function sh(e, t) {
    return Bs(4, 2, e, t)
}
function lh(e, t) {
    return Bs(4, 4, e, t)
}
function ah(e, t) {
    if (typeof t == "function")
        return e = e(),
        t(e),
        function() {
            t(null)
        }
        ;
    if (t != null)
        return e = e(),
        t.current = e,
        function() {
            t.current = null
        }
}
function uh(e, t, n) {
    return n = n != null ? n.concat([e]) : null,
    Bs(4, 4, ah.bind(null, t, e), n)
}
function Bu() {}
function ch(e, t) {
    var n = lt();
    t = t === void 0 ? null : t;
    var r = n.memoizedState;
    return r !== null && t !== null && zu(t, r[1]) ? r[0] : (n.memoizedState = [e, t],
    e)
}
function dh(e, t) {
    var n = lt();
    t = t === void 0 ? null : t;
    var r = n.memoizedState;
    return r !== null && t !== null && zu(t, r[1]) ? r[0] : (e = e(),
    n.memoizedState = [e, t],
    e)
}
function fh(e, t, n) {
    return Jn & 21 ? (yt(n, t) || (n = gp(),
    ce.lanes |= n,
    er |= n,
    e.baseState = !0),
    t) : (e.baseState && (e.baseState = !1,
    $e = !0),
    e.memoizedState = n)
}
function n0(e, t) {
    var n = J;
    J = n !== 0 && 4 > n ? n : 4,
    e(!0);
    var r = Il.transition;
    Il.transition = {};
    try {
        e(!1),
        t()
    } finally {
        J = n,
        Il.transition = r
    }
}
function ph() {
    return lt().memoizedState
}
function r0(e, t, n) {
    var r = kn(e);
    if (n = {
        lane: r,
        action: n,
        hasEagerState: !1,
        eagerState: null,
        next: null
    },
    hh(e))
        mh(t, n);
    else if (n = Yp(e, t, n, r),
    n !== null) {
        var o = Ie();
        gt(n, e, r, o),
        vh(n, t, r)
    }
}
function o0(e, t, n) {
    var r = kn(e)
      , o = {
        lane: r,
        action: n,
        hasEagerState: !1,
        eagerState: null,
        next: null
    };
    if (hh(e))
        mh(t, o);
    else {
        var i = e.alternate;
        if (e.lanes === 0 && (i === null || i.lanes === 0) && (i = t.lastRenderedReducer,
        i !== null))
            try {
                var s = t.lastRenderedState
                  , l = i(s, n);
                if (o.hasEagerState = !0,
                o.eagerState = l,
                yt(l, s)) {
                    var a = t.interleaved;
                    a === null ? (o.next = o,
                    _u(t)) : (o.next = a.next,
                    a.next = o),
                    t.interleaved = o;
                    return
                }
            } catch {} finally {}
        n = Yp(e, t, o, r),
        n !== null && (o = Ie(),
        gt(n, e, r, o),
        vh(n, t, r))
    }
}
function hh(e) {
    var t = e.alternate;
    return e === ce || t !== null && t === ce
}
function mh(e, t) {
    To = ws = !0;
    var n = e.pending;
    n === null ? t.next = t : (t.next = n.next,
    n.next = t),
    e.pending = t
}
function vh(e, t, n) {
    if (n & 4194240) {
        var r = t.lanes;
        r &= e.pendingLanes,
        n |= r,
        t.lanes = n,
        xu(e, n)
    }
}
var Es = {
    readContext: st,
    useCallback: Ne,
    useContext: Ne,
    useEffect: Ne,
    useImperativeHandle: Ne,
    useInsertionEffect: Ne,
    useLayoutEffect: Ne,
    useMemo: Ne,
    useReducer: Ne,
    useRef: Ne,
    useState: Ne,
    useDebugValue: Ne,
    useDeferredValue: Ne,
    useTransition: Ne,
    useMutableSource: Ne,
    useSyncExternalStore: Ne,
    useId: Ne,
    unstable_isNewReconciler: !1
}
  , i0 = {
    readContext: st,
    useCallback: function(e, t) {
        return Nt().memoizedState = [e, t === void 0 ? null : t],
        e
    },
    useContext: st,
    useEffect: xd,
    useImperativeHandle: function(e, t, n) {
        return n = n != null ? n.concat([e]) : null,
        Yi(4194308, 4, ah.bind(null, t, e), n)
    },
    useLayoutEffect: function(e, t) {
        return Yi(4194308, 4, e, t)
    },
    useInsertionEffect: function(e, t) {
        return Yi(4, 2, e, t)
    },
    useMemo: function(e, t) {
        var n = Nt();
        return t = t === void 0 ? null : t,
        e = e(),
        n.memoizedState = [e, t],
        e
    },
    useReducer: function(e, t, n) {
        var r = Nt();
        return t = n !== void 0 ? n(t) : t,
        r.memoizedState = r.baseState = t,
        e = {
            pending: null,
            interleaved: null,
            lanes: 0,
            dispatch: null,
            lastRenderedReducer: e,
            lastRenderedState: t
        },
        r.queue = e,
        e = e.dispatch = r0.bind(null, ce, e),
        [r.memoizedState, e]
    },
    useRef: function(e) {
        var t = Nt();
        return e = {
            current: e
        },
        t.memoizedState = e
    },
    useState: yd,
    useDebugValue: Bu,
    useDeferredValue: function(e) {
        return Nt().memoizedState = e
    },
    useTransition: function() {
        var e = yd(!1)
          , t = e[0];
        return e = n0.bind(null, e[1]),
        Nt().memoizedState = e,
        [t, e]
    },
    useMutableSource: function() {},
    useSyncExternalStore: function(e, t, n) {
        var r = ce
          , o = Nt();
        if (se) {
            if (n === void 0)
                throw Error(A(407));
            n = n()
        } else {
            if (n = t(),
            Ee === null)
                throw Error(A(349));
            Jn & 30 || eh(r, t, n)
        }
        o.memoizedState = n;
        var i = {
            value: n,
            getSnapshot: t
        };
        return o.queue = i,
        xd(nh.bind(null, r, i, e), [e]),
        r.flags |= 2048,
        Go(9, th.bind(null, r, i, n, t), void 0, null),
        n
    },
    useId: function() {
        var e = Nt()
          , t = Ee.identifierPrefix;
        if (se) {
            var n = Ut
              , r = $t;
            n = (r & ~(1 << 32 - vt(r) - 1)).toString(32) + n,
            t = ":" + t + "R" + n,
            n = Qo++,
            0 < n && (t += "H" + n.toString(32)),
            t += ":"
        } else
            n = t0++,
            t = ":" + t + "r" + n.toString(32) + ":";
        return e.memoizedState = t
    },
    unstable_isNewReconciler: !1
}
  , s0 = {
    readContext: st,
    useCallback: ch,
    useContext: st,
    useEffect: Uu,
    useImperativeHandle: uh,
    useInsertionEffect: sh,
    useLayoutEffect: lh,
    useMemo: dh,
    useReducer: Dl,
    useRef: ih,
    useState: function() {
        return Dl(Ko)
    },
    useDebugValue: Bu,
    useDeferredValue: function(e) {
        var t = lt();
        return fh(t, ye.memoizedState, e)
    },
    useTransition: function() {
        var e = Dl(Ko)[0]
          , t = lt().memoizedState;
        return [e, t]
    },
    useMutableSource: Zp,
    useSyncExternalStore: Jp,
    useId: ph,
    unstable_isNewReconciler: !1
}
  , l0 = {
    readContext: st,
    useCallback: ch,
    useContext: st,
    useEffect: Uu,
    useImperativeHandle: uh,
    useInsertionEffect: sh,
    useLayoutEffect: lh,
    useMemo: dh,
    useReducer: zl,
    useRef: ih,
    useState: function() {
        return zl(Ko)
    },
    useDebugValue: Bu,
    useDeferredValue: function(e) {
        var t = lt();
        return ye === null ? t.memoizedState = e : fh(t, ye.memoizedState, e)
    },
    useTransition: function() {
        var e = zl(Ko)[0]
          , t = lt().memoizedState;
        return [e, t]
    },
    useMutableSource: Zp,
    useSyncExternalStore: Jp,
    useId: ph,
    unstable_isNewReconciler: !1
};
function dt(e, t) {
    if (e && e.defaultProps) {
        t = de({}, t),
        e = e.defaultProps;
        for (var n in e)
            t[n] === void 0 && (t[n] = e[n]);
        return t
    }
    return t
}
function Na(e, t, n, r) {
    t = e.memoizedState,
    n = n(r, t),
    n = n == null ? t : de({}, t, n),
    e.memoizedState = n,
    e.lanes === 0 && (e.updateQueue.baseState = n)
}
var Ws = {
    isMounted: function(e) {
        return (e = e._reactInternals) ? or(e) === e : !1
    },
    enqueueSetState: function(e, t, n) {
        e = e._reactInternals;
        var r = Ie()
          , o = kn(e)
          , i = Bt(r, o);
        i.payload = t,
        n != null && (i.callback = n),
        t = Cn(e, i, o),
        t !== null && (gt(t, e, o, r),
        Ki(t, e, o))
    },
    enqueueReplaceState: function(e, t, n) {
        e = e._reactInternals;
        var r = Ie()
          , o = kn(e)
          , i = Bt(r, o);
        i.tag = 1,
        i.payload = t,
        n != null && (i.callback = n),
        t = Cn(e, i, o),
        t !== null && (gt(t, e, o, r),
        Ki(t, e, o))
    },
    enqueueForceUpdate: function(e, t) {
        e = e._reactInternals;
        var n = Ie()
          , r = kn(e)
          , o = Bt(n, r);
        o.tag = 2,
        t != null && (o.callback = t),
        t = Cn(e, o, r),
        t !== null && (gt(t, e, r, n),
        Ki(t, e, r))
    }
};
function wd(e, t, n, r, o, i, s) {
    return e = e.stateNode,
    typeof e.shouldComponentUpdate == "function" ? e.shouldComponentUpdate(r, i, s) : t.prototype && t.prototype.isPureReactComponent ? !$o(n, r) || !$o(o, i) : !0
}
function gh(e, t, n) {
    var r = !1
      , o = Rn
      , i = t.contextType;
    return typeof i == "object" && i !== null ? i = st(i) : (o = Be(t) ? qn : je.current,
    r = t.contextTypes,
    i = (r = r != null) ? Br(e, o) : Rn),
    t = new t(n,i),
    e.memoizedState = t.state !== null && t.state !== void 0 ? t.state : null,
    t.updater = Ws,
    e.stateNode = t,
    t._reactInternals = e,
    r && (e = e.stateNode,
    e.__reactInternalMemoizedUnmaskedChildContext = o,
    e.__reactInternalMemoizedMaskedChildContext = i),
    t
}
function Ed(e, t, n, r) {
    e = t.state,
    typeof t.componentWillReceiveProps == "function" && t.componentWillReceiveProps(n, r),
    typeof t.UNSAFE_componentWillReceiveProps == "function" && t.UNSAFE_componentWillReceiveProps(n, r),
    t.state !== e && Ws.enqueueReplaceState(t, t.state, null)
}
function Ta(e, t, n, r) {
    var o = e.stateNode;
    o.props = n,
    o.state = e.memoizedState,
    o.refs = {},
    Lu(e);
    var i = t.contextType;
    typeof i == "object" && i !== null ? o.context = st(i) : (i = Be(t) ? qn : je.current,
    o.context = Br(e, i)),
    o.state = e.memoizedState,
    i = t.getDerivedStateFromProps,
    typeof i == "function" && (Na(e, t, i, n),
    o.state = e.memoizedState),
    typeof t.getDerivedStateFromProps == "function" || typeof o.getSnapshotBeforeUpdate == "function" || typeof o.UNSAFE_componentWillMount != "function" && typeof o.componentWillMount != "function" || (t = o.state,
    typeof o.componentWillMount == "function" && o.componentWillMount(),
    typeof o.UNSAFE_componentWillMount == "function" && o.UNSAFE_componentWillMount(),
    t !== o.state && Ws.enqueueReplaceState(o, o.state, null),
    ys(e, n, o, r),
    o.state = e.memoizedState),
    typeof o.componentDidMount == "function" && (e.flags |= 4194308)
}
function Qr(e, t) {
    try {
        var n = ""
          , r = t;
        do
            n += Mg(r),
            r = r.return;
        while (r);
        var o = n
    } catch (i) {
        o = `
Error generating stack: ` + i.message + `
` + i.stack
    }
    return {
        value: e,
        source: t,
        stack: o,
        digest: null
    }
}
function Fl(e, t, n) {
    return {
        value: e,
        source: null,
        stack: n ?? null,
        digest: t ?? null
    }
}
function Ra(e, t) {
    try {
        console.error(t.value)
    } catch (n) {
        setTimeout(function() {
            throw n
        })
    }
}
var a0 = typeof WeakMap == "function" ? WeakMap : Map;
function yh(e, t, n) {
    n = Bt(-1, n),
    n.tag = 3,
    n.payload = {
        element: null
    };
    var r = t.value;
    return n.callback = function() {
        Cs || (Cs = !0,
        Fa = r),
        Ra(e, t)
    }
    ,
    n
}
function xh(e, t, n) {
    n = Bt(-1, n),
    n.tag = 3;
    var r = e.type.getDerivedStateFromError;
    if (typeof r == "function") {
        var o = t.value;
        n.payload = function() {
            return r(o)
        }
        ,
        n.callback = function() {
            Ra(e, t)
        }
    }
    var i = e.stateNode;
    return i !== null && typeof i.componentDidCatch == "function" && (n.callback = function() {
        Ra(e, t),
        typeof r != "function" && (bn === null ? bn = new Set([this]) : bn.add(this));
        var s = t.stack;
        this.componentDidCatch(t.value, {
            componentStack: s !== null ? s : ""
        })
    }
    ),
    n
}
function Sd(e, t, n) {
    var r = e.pingCache;
    if (r === null) {
        r = e.pingCache = new a0;
        var o = new Set;
        r.set(t, o)
    } else
        o = r.get(t),
        o === void 0 && (o = new Set,
        r.set(t, o));
    o.has(n) || (o.add(n),
    e = S0.bind(null, e, t, n),
    t.then(e, e))
}
function Cd(e) {
    do {
        var t;
        if ((t = e.tag === 13) && (t = e.memoizedState,
        t = t !== null ? t.dehydrated !== null : !0),
        t)
            return e;
        e = e.return
    } while (e !== null);
    return null
}
function bd(e, t, n, r, o) {
    return e.mode & 1 ? (e.flags |= 65536,
    e.lanes = o,
    e) : (e === t ? e.flags |= 65536 : (e.flags |= 128,
    n.flags |= 131072,
    n.flags &= -52805,
    n.tag === 1 && (n.alternate === null ? n.tag = 17 : (t = Bt(-1, 1),
    t.tag = 2,
    Cn(n, t, 1))),
    n.lanes |= 1),
    e)
}
var u0 = Yt.ReactCurrentOwner
  , $e = !1;
function Le(e, t, n, r) {
    t.child = e === null ? Gp(t, null, n, r) : Vr(t, e.child, n, r)
}
function kd(e, t, n, r, o) {
    n = n.render;
    var i = t.ref;
    return Tr(t, o),
    r = Fu(e, t, n, r, i, o),
    n = $u(),
    e !== null && !$e ? (t.updateQueue = e.updateQueue,
    t.flags &= -2053,
    e.lanes &= ~o,
    Qt(e, t, o)) : (se && n && Nu(t),
    t.flags |= 1,
    Le(e, t, r, o),
    t.child)
}
function Pd(e, t, n, r, o) {
    if (e === null) {
        var i = n.type;
        return typeof i == "function" && !Xu(i) && i.defaultProps === void 0 && n.compare === null && n.defaultProps === void 0 ? (t.tag = 15,
        t.type = i,
        wh(e, t, i, r, o)) : (e = Ji(n.type, null, r, t, t.mode, o),
        e.ref = t.ref,
        e.return = t,
        t.child = e)
    }
    if (i = e.child,
    !(e.lanes & o)) {
        var s = i.memoizedProps;
        if (n = n.compare,
        n = n !== null ? n : $o,
        n(s, r) && e.ref === t.ref)
            return Qt(e, t, o)
    }
    return t.flags |= 1,
    e = Pn(i, r),
    e.ref = t.ref,
    e.return = t,
    t.child = e
}
function wh(e, t, n, r, o) {
    if (e !== null) {
        var i = e.memoizedProps;
        if ($o(i, r) && e.ref === t.ref)
            if ($e = !1,
            t.pendingProps = r = i,
            (e.lanes & o) !== 0)
                e.flags & 131072 && ($e = !0);
            else
                return t.lanes = e.lanes,
                Qt(e, t, o)
    }
    return Aa(e, t, n, r, o)
}
function Eh(e, t, n) {
    var r = t.pendingProps
      , o = r.children
      , i = e !== null ? e.memoizedState : null;
    if (r.mode === "hidden")
        if (!(t.mode & 1))
            t.memoizedState = {
                baseLanes: 0,
                cachePool: null,
                transitions: null
            },
            te(Cr, Ke),
            Ke |= n;
        else {
            if (!(n & 1073741824))
                return e = i !== null ? i.baseLanes | n : n,
                t.lanes = t.childLanes = 1073741824,
                t.memoizedState = {
                    baseLanes: e,
                    cachePool: null,
                    transitions: null
                },
                t.updateQueue = null,
                te(Cr, Ke),
                Ke |= e,
                null;
            t.memoizedState = {
                baseLanes: 0,
                cachePool: null,
                transitions: null
            },
            r = i !== null ? i.baseLanes : n,
            te(Cr, Ke),
            Ke |= r
        }
    else
        i !== null ? (r = i.baseLanes | n,
        t.memoizedState = null) : r = n,
        te(Cr, Ke),
        Ke |= r;
    return Le(e, t, o, n),
    t.child
}
function Sh(e, t) {
    var n = t.ref;
    (e === null && n !== null || e !== null && e.ref !== n) && (t.flags |= 512,
    t.flags |= 2097152)
}
function Aa(e, t, n, r, o) {
    var i = Be(n) ? qn : je.current;
    return i = Br(t, i),
    Tr(t, o),
    n = Fu(e, t, n, r, i, o),
    r = $u(),
    e !== null && !$e ? (t.updateQueue = e.updateQueue,
    t.flags &= -2053,
    e.lanes &= ~o,
    Qt(e, t, o)) : (se && r && Nu(t),
    t.flags |= 1,
    Le(e, t, n, o),
    t.child)
}
function Nd(e, t, n, r, o) {
    if (Be(n)) {
        var i = !0;
        ps(t)
    } else
        i = !1;
    if (Tr(t, o),
    t.stateNode === null)
        Xi(e, t),
        gh(t, n, r),
        Ta(t, n, r, o),
        r = !0;
    else if (e === null) {
        var s = t.stateNode
          , l = t.memoizedProps;
        s.props = l;
        var a = s.context
          , u = n.contextType;
        typeof u == "object" && u !== null ? u = st(u) : (u = Be(n) ? qn : je.current,
        u = Br(t, u));
        var d = n.getDerivedStateFromProps
          , f = typeof d == "function" || typeof s.getSnapshotBeforeUpdate == "function";
        f || typeof s.UNSAFE_componentWillReceiveProps != "function" && typeof s.componentWillReceiveProps != "function" || (l !== r || a !== u) && Ed(t, s, r, u),
        an = !1;
        var c = t.memoizedState;
        s.state = c,
        ys(t, r, s, o),
        a = t.memoizedState,
        l !== r || c !== a || Ue.current || an ? (typeof d == "function" && (Na(t, n, d, r),
        a = t.memoizedState),
        (l = an || wd(t, n, l, r, c, a, u)) ? (f || typeof s.UNSAFE_componentWillMount != "function" && typeof s.componentWillMount != "function" || (typeof s.componentWillMount == "function" && s.componentWillMount(),
        typeof s.UNSAFE_componentWillMount == "function" && s.UNSAFE_componentWillMount()),
        typeof s.componentDidMount == "function" && (t.flags |= 4194308)) : (typeof s.componentDidMount == "function" && (t.flags |= 4194308),
        t.memoizedProps = r,
        t.memoizedState = a),
        s.props = r,
        s.state = a,
        s.context = u,
        r = l) : (typeof s.componentDidMount == "function" && (t.flags |= 4194308),
        r = !1)
    } else {
        s = t.stateNode,
        Xp(e, t),
        l = t.memoizedProps,
        u = t.type === t.elementType ? l : dt(t.type, l),
        s.props = u,
        f = t.pendingProps,
        c = s.context,
        a = n.contextType,
        typeof a == "object" && a !== null ? a = st(a) : (a = Be(n) ? qn : je.current,
        a = Br(t, a));
        var y = n.getDerivedStateFromProps;
        (d = typeof y == "function" || typeof s.getSnapshotBeforeUpdate == "function") || typeof s.UNSAFE_componentWillReceiveProps != "function" && typeof s.componentWillReceiveProps != "function" || (l !== f || c !== a) && Ed(t, s, r, a),
        an = !1,
        c = t.memoizedState,
        s.state = c,
        ys(t, r, s, o);
        var w = t.memoizedState;
        l !== f || c !== w || Ue.current || an ? (typeof y == "function" && (Na(t, n, y, r),
        w = t.memoizedState),
        (u = an || wd(t, n, u, r, c, w, a) || !1) ? (d || typeof s.UNSAFE_componentWillUpdate != "function" && typeof s.componentWillUpdate != "function" || (typeof s.componentWillUpdate == "function" && s.componentWillUpdate(r, w, a),
        typeof s.UNSAFE_componentWillUpdate == "function" && s.UNSAFE_componentWillUpdate(r, w, a)),
        typeof s.componentDidUpdate == "function" && (t.flags |= 4),
        typeof s.getSnapshotBeforeUpdate == "function" && (t.flags |= 1024)) : (typeof s.componentDidUpdate != "function" || l === e.memoizedProps && c === e.memoizedState || (t.flags |= 4),
        typeof s.getSnapshotBeforeUpdate != "function" || l === e.memoizedProps && c === e.memoizedState || (t.flags |= 1024),
        t.memoizedProps = r,
        t.memoizedState = w),
        s.props = r,
        s.state = w,
        s.context = a,
        r = u) : (typeof s.componentDidUpdate != "function" || l === e.memoizedProps && c === e.memoizedState || (t.flags |= 4),
        typeof s.getSnapshotBeforeUpdate != "function" || l === e.memoizedProps && c === e.memoizedState || (t.flags |= 1024),
        r = !1)
    }
    return ja(e, t, n, r, i, o)
}
function ja(e, t, n, r, o, i) {
    Sh(e, t);
    var s = (t.flags & 128) !== 0;
    if (!r && !s)
        return o && fd(t, n, !1),
        Qt(e, t, i);
    r = t.stateNode,
    u0.current = t;
    var l = s && typeof n.getDerivedStateFromError != "function" ? null : r.render();
    return t.flags |= 1,
    e !== null && s ? (t.child = Vr(t, e.child, null, i),
    t.child = Vr(t, null, l, i)) : Le(e, t, l, i),
    t.memoizedState = r.state,
    o && fd(t, n, !0),
    t.child
}
function Ch(e) {
    var t = e.stateNode;
    t.pendingContext ? dd(e, t.pendingContext, t.pendingContext !== t.context) : t.context && dd(e, t.context, !1),
    Mu(e, t.containerInfo)
}
function Td(e, t, n, r, o) {
    return Wr(),
    Ru(o),
    t.flags |= 256,
    Le(e, t, n, r),
    t.child
}
var Oa = {
    dehydrated: null,
    treeContext: null,
    retryLane: 0
};
function _a(e) {
    return {
        baseLanes: e,
        cachePool: null,
        transitions: null
    }
}
function bh(e, t, n) {
    var r = t.pendingProps, o = ue.current, i = !1, s = (t.flags & 128) !== 0, l;
    if ((l = s) || (l = e !== null && e.memoizedState === null ? !1 : (o & 2) !== 0),
    l ? (i = !0,
    t.flags &= -129) : (e === null || e.memoizedState !== null) && (o |= 1),
    te(ue, o & 1),
    e === null)
        return ka(t),
        e = t.memoizedState,
        e !== null && (e = e.dehydrated,
        e !== null) ? (t.mode & 1 ? e.data === "$!" ? t.lanes = 8 : t.lanes = 1073741824 : t.lanes = 1,
        null) : (s = r.children,
        e = r.fallback,
        i ? (r = t.mode,
        i = t.child,
        s = {
            mode: "hidden",
            children: s
        },
        !(r & 1) && i !== null ? (i.childLanes = 0,
        i.pendingProps = s) : i = Qs(s, r, 0, null),
        e = Xn(e, r, n, null),
        i.return = t,
        e.return = t,
        i.sibling = e,
        t.child = i,
        t.child.memoizedState = _a(n),
        t.memoizedState = Oa,
        e) : Wu(t, s));
    if (o = e.memoizedState,
    o !== null && (l = o.dehydrated,
    l !== null))
        return c0(e, t, s, r, l, o, n);
    if (i) {
        i = r.fallback,
        s = t.mode,
        o = e.child,
        l = o.sibling;
        var a = {
            mode: "hidden",
            children: r.children
        };
        return !(s & 1) && t.child !== o ? (r = t.child,
        r.childLanes = 0,
        r.pendingProps = a,
        t.deletions = null) : (r = Pn(o, a),
        r.subtreeFlags = o.subtreeFlags & 14680064),
        l !== null ? i = Pn(l, i) : (i = Xn(i, s, n, null),
        i.flags |= 2),
        i.return = t,
        r.return = t,
        r.sibling = i,
        t.child = r,
        r = i,
        i = t.child,
        s = e.child.memoizedState,
        s = s === null ? _a(n) : {
            baseLanes: s.baseLanes | n,
            cachePool: null,
            transitions: s.transitions
        },
        i.memoizedState = s,
        i.childLanes = e.childLanes & ~n,
        t.memoizedState = Oa,
        r
    }
    return i = e.child,
    e = i.sibling,
    r = Pn(i, {
        mode: "visible",
        children: r.children
    }),
    !(t.mode & 1) && (r.lanes = n),
    r.return = t,
    r.sibling = null,
    e !== null && (n = t.deletions,
    n === null ? (t.deletions = [e],
    t.flags |= 16) : n.push(e)),
    t.child = r,
    t.memoizedState = null,
    r
}
function Wu(e, t) {
    return t = Qs({
        mode: "visible",
        children: t
    }, e.mode, 0, null),
    t.return = e,
    e.child = t
}
function Oi(e, t, n, r) {
    return r !== null && Ru(r),
    Vr(t, e.child, null, n),
    e = Wu(t, t.pendingProps.children),
    e.flags |= 2,
    t.memoizedState = null,
    e
}
function c0(e, t, n, r, o, i, s) {
    if (n)
        return t.flags & 256 ? (t.flags &= -257,
        r = Fl(Error(A(422))),
        Oi(e, t, s, r)) : t.memoizedState !== null ? (t.child = e.child,
        t.flags |= 128,
        null) : (i = r.fallback,
        o = t.mode,
        r = Qs({
            mode: "visible",
            children: r.children
        }, o, 0, null),
        i = Xn(i, o, s, null),
        i.flags |= 2,
        r.return = t,
        i.return = t,
        r.sibling = i,
        t.child = r,
        t.mode & 1 && Vr(t, e.child, null, s),
        t.child.memoizedState = _a(s),
        t.memoizedState = Oa,
        i);
    if (!(t.mode & 1))
        return Oi(e, t, s, null);
    if (o.data === "$!") {
        if (r = o.nextSibling && o.nextSibling.dataset,
        r)
            var l = r.dgst;
        return r = l,
        i = Error(A(419)),
        r = Fl(i, r, void 0),
        Oi(e, t, s, r)
    }
    if (l = (s & e.childLanes) !== 0,
    $e || l) {
        if (r = Ee,
        r !== null) {
            switch (s & -s) {
            case 4:
                o = 2;
                break;
            case 16:
                o = 8;
                break;
            case 64:
            case 128:
            case 256:
            case 512:
            case 1024:
            case 2048:
            case 4096:
            case 8192:
            case 16384:
            case 32768:
            case 65536:
            case 131072:
            case 262144:
            case 524288:
            case 1048576:
            case 2097152:
            case 4194304:
            case 8388608:
            case 16777216:
            case 33554432:
            case 67108864:
                o = 32;
                break;
            case 536870912:
                o = 268435456;
                break;
            default:
                o = 0
            }
            o = o & (r.suspendedLanes | s) ? 0 : o,
            o !== 0 && o !== i.retryLane && (i.retryLane = o,
            Ht(e, o),
            gt(r, e, o, -1))
        }
        return Yu(),
        r = Fl(Error(A(421))),
        Oi(e, t, s, r)
    }
    return o.data === "$?" ? (t.flags |= 128,
    t.child = e.child,
    t = C0.bind(null, e),
    o._reactRetry = t,
    null) : (e = i.treeContext,
    Ye = Sn(o.nextSibling),
    Xe = t,
    se = !0,
    mt = null,
    e !== null && (nt[rt++] = $t,
    nt[rt++] = Ut,
    nt[rt++] = Zn,
    $t = e.id,
    Ut = e.overflow,
    Zn = t),
    t = Wu(t, r.children),
    t.flags |= 4096,
    t)
}
function Rd(e, t, n) {
    e.lanes |= t;
    var r = e.alternate;
    r !== null && (r.lanes |= t),
    Pa(e.return, t, n)
}
function $l(e, t, n, r, o) {
    var i = e.memoizedState;
    i === null ? e.memoizedState = {
        isBackwards: t,
        rendering: null,
        renderingStartTime: 0,
        last: r,
        tail: n,
        tailMode: o
    } : (i.isBackwards = t,
    i.rendering = null,
    i.renderingStartTime = 0,
    i.last = r,
    i.tail = n,
    i.tailMode = o)
}
function kh(e, t, n) {
    var r = t.pendingProps
      , o = r.revealOrder
      , i = r.tail;
    if (Le(e, t, r.children, n),
    r = ue.current,
    r & 2)
        r = r & 1 | 2,
        t.flags |= 128;
    else {
        if (e !== null && e.flags & 128)
            e: for (e = t.child; e !== null; ) {
                if (e.tag === 13)
                    e.memoizedState !== null && Rd(e, n, t);
                else if (e.tag === 19)
                    Rd(e, n, t);
                else if (e.child !== null) {
                    e.child.return = e,
                    e = e.child;
                    continue
                }
                if (e === t)
                    break e;
                for (; e.sibling === null; ) {
                    if (e.return === null || e.return === t)
                        break e;
                    e = e.return
                }
                e.sibling.return = e.return,
                e = e.sibling
            }
        r &= 1
    }
    if (te(ue, r),
    !(t.mode & 1))
        t.memoizedState = null;
    else
        switch (o) {
        case "forwards":
            for (n = t.child,
            o = null; n !== null; )
                e = n.alternate,
                e !== null && xs(e) === null && (o = n),
                n = n.sibling;
            n = o,
            n === null ? (o = t.child,
            t.child = null) : (o = n.sibling,
            n.sibling = null),
            $l(t, !1, o, n, i);
            break;
        case "backwards":
            for (n = null,
            o = t.child,
            t.child = null; o !== null; ) {
                if (e = o.alternate,
                e !== null && xs(e) === null) {
                    t.child = o;
                    break
                }
                e = o.sibling,
                o.sibling = n,
                n = o,
                o = e
            }
            $l(t, !0, n, null, i);
            break;
        case "together":
            $l(t, !1, null, null, void 0);
            break;
        default:
            t.memoizedState = null
        }
    return t.child
}
function Xi(e, t) {
    !(t.mode & 1) && e !== null && (e.alternate = null,
    t.alternate = null,
    t.flags |= 2)
}
function Qt(e, t, n) {
    if (e !== null && (t.dependencies = e.dependencies),
    er |= t.lanes,
    !(n & t.childLanes))
        return null;
    if (e !== null && t.child !== e.child)
        throw Error(A(153));
    if (t.child !== null) {
        for (e = t.child,
        n = Pn(e, e.pendingProps),
        t.child = n,
        n.return = t; e.sibling !== null; )
            e = e.sibling,
            n = n.sibling = Pn(e, e.pendingProps),
            n.return = t;
        n.sibling = null
    }
    return t.child
}
function d0(e, t, n) {
    switch (t.tag) {
    case 3:
        Ch(t),
        Wr();
        break;
    case 5:
        qp(t);
        break;
    case 1:
        Be(t.type) && ps(t);
        break;
    case 4:
        Mu(t, t.stateNode.containerInfo);
        break;
    case 10:
        var r = t.type._context
          , o = t.memoizedProps.value;
        te(vs, r._currentValue),
        r._currentValue = o;
        break;
    case 13:
        if (r = t.memoizedState,
        r !== null)
            return r.dehydrated !== null ? (te(ue, ue.current & 1),
            t.flags |= 128,
            null) : n & t.child.childLanes ? bh(e, t, n) : (te(ue, ue.current & 1),
            e = Qt(e, t, n),
            e !== null ? e.sibling : null);
        te(ue, ue.current & 1);
        break;
    case 19:
        if (r = (n & t.childLanes) !== 0,
        e.flags & 128) {
            if (r)
                return kh(e, t, n);
            t.flags |= 128
        }
        if (o = t.memoizedState,
        o !== null && (o.rendering = null,
        o.tail = null,
        o.lastEffect = null),
        te(ue, ue.current),
        r)
            break;
        return null;
    case 22:
    case 23:
        return t.lanes = 0,
        Eh(e, t, n)
    }
    return Qt(e, t, n)
}
var Ph, La, Nh, Th;
Ph = function(e, t) {
    for (var n = t.child; n !== null; ) {
        if (n.tag === 5 || n.tag === 6)
            e.appendChild(n.stateNode);
        else if (n.tag !== 4 && n.child !== null) {
            n.child.return = n,
            n = n.child;
            continue
        }
        if (n === t)
            break;
        for (; n.sibling === null; ) {
            if (n.return === null || n.return === t)
                return;
            n = n.return
        }
        n.sibling.return = n.return,
        n = n.sibling
    }
}
;
La = function() {}
;
Nh = function(e, t, n, r) {
    var o = e.memoizedProps;
    if (o !== r) {
        e = t.stateNode,
        Vn(_t.current);
        var i = null;
        switch (n) {
        case "input":
            o = na(e, o),
            r = na(e, r),
            i = [];
            break;
        case "select":
            o = de({}, o, {
                value: void 0
            }),
            r = de({}, r, {
                value: void 0
            }),
            i = [];
            break;
        case "textarea":
            o = ia(e, o),
            r = ia(e, r),
            i = [];
            break;
        default:
            typeof o.onClick != "function" && typeof r.onClick == "function" && (e.onclick = ds)
        }
        la(n, r);
        var s;
        n = null;
        for (u in o)
            if (!r.hasOwnProperty(u) && o.hasOwnProperty(u) && o[u] != null)
                if (u === "style") {
                    var l = o[u];
                    for (s in l)
                        l.hasOwnProperty(s) && (n || (n = {}),
                        n[s] = "")
                } else
                    u !== "dangerouslySetInnerHTML" && u !== "children" && u !== "suppressContentEditableWarning" && u !== "suppressHydrationWarning" && u !== "autoFocus" && (_o.hasOwnProperty(u) ? i || (i = []) : (i = i || []).push(u, null));
        for (u in r) {
            var a = r[u];
            if (l = o != null ? o[u] : void 0,
            r.hasOwnProperty(u) && a !== l && (a != null || l != null))
                if (u === "style")
                    if (l) {
                        for (s in l)
                            !l.hasOwnProperty(s) || a && a.hasOwnProperty(s) || (n || (n = {}),
                            n[s] = "");
                        for (s in a)
                            a.hasOwnProperty(s) && l[s] !== a[s] && (n || (n = {}),
                            n[s] = a[s])
                    } else
                        n || (i || (i = []),
                        i.push(u, n)),
                        n = a;
                else
                    u === "dangerouslySetInnerHTML" ? (a = a ? a.__html : void 0,
                    l = l ? l.__html : void 0,
                    a != null && l !== a && (i = i || []).push(u, a)) : u === "children" ? typeof a != "string" && typeof a != "number" || (i = i || []).push(u, "" + a) : u !== "suppressContentEditableWarning" && u !== "suppressHydrationWarning" && (_o.hasOwnProperty(u) ? (a != null && u === "onScroll" && oe("scroll", e),
                    i || l === a || (i = [])) : (i = i || []).push(u, a))
        }
        n && (i = i || []).push("style", n);
        var u = i;
        (t.updateQueue = u) && (t.flags |= 4)
    }
}
;
Th = function(e, t, n, r) {
    n !== r && (t.flags |= 4)
}
;
function ho(e, t) {
    if (!se)
        switch (e.tailMode) {
        case "hidden":
            t = e.tail;
            for (var n = null; t !== null; )
                t.alternate !== null && (n = t),
                t = t.sibling;
            n === null ? e.tail = null : n.sibling = null;
            break;
        case "collapsed":
            n = e.tail;
            for (var r = null; n !== null; )
                n.alternate !== null && (r = n),
                n = n.sibling;
            r === null ? t || e.tail === null ? e.tail = null : e.tail.sibling = null : r.sibling = null
        }
}
function Te(e) {
    var t = e.alternate !== null && e.alternate.child === e.child
      , n = 0
      , r = 0;
    if (t)
        for (var o = e.child; o !== null; )
            n |= o.lanes | o.childLanes,
            r |= o.subtreeFlags & 14680064,
            r |= o.flags & 14680064,
            o.return = e,
            o = o.sibling;
    else
        for (o = e.child; o !== null; )
            n |= o.lanes | o.childLanes,
            r |= o.subtreeFlags,
            r |= o.flags,
            o.return = e,
            o = o.sibling;
    return e.subtreeFlags |= r,
    e.childLanes = n,
    t
}
function f0(e, t, n) {
    var r = t.pendingProps;
    switch (Tu(t),
    t.tag) {
    case 2:
    case 16:
    case 15:
    case 0:
    case 11:
    case 7:
    case 8:
    case 12:
    case 9:
    case 14:
        return Te(t),
        null;
    case 1:
        return Be(t.type) && fs(),
        Te(t),
        null;
    case 3:
        return r = t.stateNode,
        Hr(),
        ie(Ue),
        ie(je),
        Du(),
        r.pendingContext && (r.context = r.pendingContext,
        r.pendingContext = null),
        (e === null || e.child === null) && (Ai(t) ? t.flags |= 4 : e === null || e.memoizedState.isDehydrated && !(t.flags & 256) || (t.flags |= 1024,
        mt !== null && (Ba(mt),
        mt = null))),
        La(e, t),
        Te(t),
        null;
    case 5:
        Iu(t);
        var o = Vn(Ho.current);
        if (n = t.type,
        e !== null && t.stateNode != null)
            Nh(e, t, n, r, o),
            e.ref !== t.ref && (t.flags |= 512,
            t.flags |= 2097152);
        else {
            if (!r) {
                if (t.stateNode === null)
                    throw Error(A(166));
                return Te(t),
                null
            }
            if (e = Vn(_t.current),
            Ai(t)) {
                r = t.stateNode,
                n = t.type;
                var i = t.memoizedProps;
                switch (r[jt] = t,
                r[Wo] = i,
                e = (t.mode & 1) !== 0,
                n) {
                case "dialog":
                    oe("cancel", r),
                    oe("close", r);
                    break;
                case "iframe":
                case "object":
                case "embed":
                    oe("load", r);
                    break;
                case "video":
                case "audio":
                    for (o = 0; o < So.length; o++)
                        oe(So[o], r);
                    break;
                case "source":
                    oe("error", r);
                    break;
                case "img":
                case "image":
                case "link":
                    oe("error", r),
                    oe("load", r);
                    break;
                case "details":
                    oe("toggle", r);
                    break;
                case "input":
                    zc(r, i),
                    oe("invalid", r);
                    break;
                case "select":
                    r._wrapperState = {
                        wasMultiple: !!i.multiple
                    },
                    oe("invalid", r);
                    break;
                case "textarea":
                    $c(r, i),
                    oe("invalid", r)
                }
                la(n, i),
                o = null;
                for (var s in i)
                    if (i.hasOwnProperty(s)) {
                        var l = i[s];
                        s === "children" ? typeof l == "string" ? r.textContent !== l && (i.suppressHydrationWarning !== !0 && Ri(r.textContent, l, e),
                        o = ["children", l]) : typeof l == "number" && r.textContent !== "" + l && (i.suppressHydrationWarning !== !0 && Ri(r.textContent, l, e),
                        o = ["children", "" + l]) : _o.hasOwnProperty(s) && l != null && s === "onScroll" && oe("scroll", r)
                    }
                switch (n) {
                case "input":
                    Ei(r),
                    Fc(r, i, !0);
                    break;
                case "textarea":
                    Ei(r),
                    Uc(r);
                    break;
                case "select":
                case "option":
                    break;
                default:
                    typeof i.onClick == "function" && (r.onclick = ds)
                }
                r = o,
                t.updateQueue = r,
                r !== null && (t.flags |= 4)
            } else {
                s = o.nodeType === 9 ? o : o.ownerDocument,
                e === "http://www.w3.org/1999/xhtml" && (e = np(n)),
                e === "http://www.w3.org/1999/xhtml" ? n === "script" ? (e = s.createElement("div"),
                e.innerHTML = "<script><\/script>",
                e = e.removeChild(e.firstChild)) : typeof r.is == "string" ? e = s.createElement(n, {
                    is: r.is
                }) : (e = s.createElement(n),
                n === "select" && (s = e,
                r.multiple ? s.multiple = !0 : r.size && (s.size = r.size))) : e = s.createElementNS(e, n),
                e[jt] = t,
                e[Wo] = r,
                Ph(e, t, !1, !1),
                t.stateNode = e;
                e: {
                    switch (s = aa(n, r),
                    n) {
                    case "dialog":
                        oe("cancel", e),
                        oe("close", e),
                        o = r;
                        break;
                    case "iframe":
                    case "object":
                    case "embed":
                        oe("load", e),
                        o = r;
                        break;
                    case "video":
                    case "audio":
                        for (o = 0; o < So.length; o++)
                            oe(So[o], e);
                        o = r;
                        break;
                    case "source":
                        oe("error", e),
                        o = r;
                        break;
                    case "img":
                    case "image":
                    case "link":
                        oe("error", e),
                        oe("load", e),
                        o = r;
                        break;
                    case "details":
                        oe("toggle", e),
                        o = r;
                        break;
                    case "input":
                        zc(e, r),
                        o = na(e, r),
                        oe("invalid", e);
                        break;
                    case "option":
                        o = r;
                        break;
                    case "select":
                        e._wrapperState = {
                            wasMultiple: !!r.multiple
                        },
                        o = de({}, r, {
                            value: void 0
                        }),
                        oe("invalid", e);
                        break;
                    case "textarea":
                        $c(e, r),
                        o = ia(e, r),
                        oe("invalid", e);
                        break;
                    default:
                        o = r
                    }
                    la(n, o),
                    l = o;
                    for (i in l)
                        if (l.hasOwnProperty(i)) {
                            var a = l[i];
                            i === "style" ? ip(e, a) : i === "dangerouslySetInnerHTML" ? (a = a ? a.__html : void 0,
                            a != null && rp(e, a)) : i === "children" ? typeof a == "string" ? (n !== "textarea" || a !== "") && Lo(e, a) : typeof a == "number" && Lo(e, "" + a) : i !== "suppressContentEditableWarning" && i !== "suppressHydrationWarning" && i !== "autoFocus" && (_o.hasOwnProperty(i) ? a != null && i === "onScroll" && oe("scroll", e) : a != null && pu(e, i, a, s))
                        }
                    switch (n) {
                    case "input":
                        Ei(e),
                        Fc(e, r, !1);
                        break;
                    case "textarea":
                        Ei(e),
                        Uc(e);
                        break;
                    case "option":
                        r.value != null && e.setAttribute("value", "" + Tn(r.value));
                        break;
                    case "select":
                        e.multiple = !!r.multiple,
                        i = r.value,
                        i != null ? br(e, !!r.multiple, i, !1) : r.defaultValue != null && br(e, !!r.multiple, r.defaultValue, !0);
                        break;
                    default:
                        typeof o.onClick == "function" && (e.onclick = ds)
                    }
                    switch (n) {
                    case "button":
                    case "input":
                    case "select":
                    case "textarea":
                        r = !!r.autoFocus;
                        break e;
                    case "img":
                        r = !0;
                        break e;
                    default:
                        r = !1
                    }
                }
                r && (t.flags |= 4)
            }
            t.ref !== null && (t.flags |= 512,
            t.flags |= 2097152)
        }
        return Te(t),
        null;
    case 6:
        if (e && t.stateNode != null)
            Th(e, t, e.memoizedProps, r);
        else {
            if (typeof r != "string" && t.stateNode === null)
                throw Error(A(166));
            if (n = Vn(Ho.current),
            Vn(_t.current),
            Ai(t)) {
                if (r = t.stateNode,
                n = t.memoizedProps,
                r[jt] = t,
                (i = r.nodeValue !== n) && (e = Xe,
                e !== null))
                    switch (e.tag) {
                    case 3:
                        Ri(r.nodeValue, n, (e.mode & 1) !== 0);
                        break;
                    case 5:
                        e.memoizedProps.suppressHydrationWarning !== !0 && Ri(r.nodeValue, n, (e.mode & 1) !== 0)
                    }
                i && (t.flags |= 4)
            } else
                r = (n.nodeType === 9 ? n : n.ownerDocument).createTextNode(r),
                r[jt] = t,
                t.stateNode = r
        }
        return Te(t),
        null;
    case 13:
        if (ie(ue),
        r = t.memoizedState,
        e === null || e.memoizedState !== null && e.memoizedState.dehydrated !== null) {
            if (se && Ye !== null && t.mode & 1 && !(t.flags & 128))
                Qp(),
                Wr(),
                t.flags |= 98560,
                i = !1;
            else if (i = Ai(t),
            r !== null && r.dehydrated !== null) {
                if (e === null) {
                    if (!i)
                        throw Error(A(318));
                    if (i = t.memoizedState,
                    i = i !== null ? i.dehydrated : null,
                    !i)
                        throw Error(A(317));
                    i[jt] = t
                } else
                    Wr(),
                    !(t.flags & 128) && (t.memoizedState = null),
                    t.flags |= 4;
                Te(t),
                i = !1
            } else
                mt !== null && (Ba(mt),
                mt = null),
                i = !0;
            if (!i)
                return t.flags & 65536 ? t : null
        }
        return t.flags & 128 ? (t.lanes = n,
        t) : (r = r !== null,
        r !== (e !== null && e.memoizedState !== null) && r && (t.child.flags |= 8192,
        t.mode & 1 && (e === null || ue.current & 1 ? xe === 0 && (xe = 3) : Yu())),
        t.updateQueue !== null && (t.flags |= 4),
        Te(t),
        null);
    case 4:
        return Hr(),
        La(e, t),
        e === null && Uo(t.stateNode.containerInfo),
        Te(t),
        null;
    case 10:
        return Ou(t.type._context),
        Te(t),
        null;
    case 17:
        return Be(t.type) && fs(),
        Te(t),
        null;
    case 19:
        if (ie(ue),
        i = t.memoizedState,
        i === null)
            return Te(t),
            null;
        if (r = (t.flags & 128) !== 0,
        s = i.rendering,
        s === null)
            if (r)
                ho(i, !1);
            else {
                if (xe !== 0 || e !== null && e.flags & 128)
                    for (e = t.child; e !== null; ) {
                        if (s = xs(e),
                        s !== null) {
                            for (t.flags |= 128,
                            ho(i, !1),
                            r = s.updateQueue,
                            r !== null && (t.updateQueue = r,
                            t.flags |= 4),
                            t.subtreeFlags = 0,
                            r = n,
                            n = t.child; n !== null; )
                                i = n,
                                e = r,
                                i.flags &= 14680066,
                                s = i.alternate,
                                s === null ? (i.childLanes = 0,
                                i.lanes = e,
                                i.child = null,
                                i.subtreeFlags = 0,
                                i.memoizedProps = null,
                                i.memoizedState = null,
                                i.updateQueue = null,
                                i.dependencies = null,
                                i.stateNode = null) : (i.childLanes = s.childLanes,
                                i.lanes = s.lanes,
                                i.child = s.child,
                                i.subtreeFlags = 0,
                                i.deletions = null,
                                i.memoizedProps = s.memoizedProps,
                                i.memoizedState = s.memoizedState,
                                i.updateQueue = s.updateQueue,
                                i.type = s.type,
                                e = s.dependencies,
                                i.dependencies = e === null ? null : {
                                    lanes: e.lanes,
                                    firstContext: e.firstContext
                                }),
                                n = n.sibling;
                            return te(ue, ue.current & 1 | 2),
                            t.child
                        }
                        e = e.sibling
                    }
                i.tail !== null && he() > Kr && (t.flags |= 128,
                r = !0,
                ho(i, !1),
                t.lanes = 4194304)
            }
        else {
            if (!r)
                if (e = xs(s),
                e !== null) {
                    if (t.flags |= 128,
                    r = !0,
                    n = e.updateQueue,
                    n !== null && (t.updateQueue = n,
                    t.flags |= 4),
                    ho(i, !0),
                    i.tail === null && i.tailMode === "hidden" && !s.alternate && !se)
                        return Te(t),
                        null
                } else
                    2 * he() - i.renderingStartTime > Kr && n !== 1073741824 && (t.flags |= 128,
                    r = !0,
                    ho(i, !1),
                    t.lanes = 4194304);
            i.isBackwards ? (s.sibling = t.child,
            t.child = s) : (n = i.last,
            n !== null ? n.sibling = s : t.child = s,
            i.last = s)
        }
        return i.tail !== null ? (t = i.tail,
        i.rendering = t,
        i.tail = t.sibling,
        i.renderingStartTime = he(),
        t.sibling = null,
        n = ue.current,
        te(ue, r ? n & 1 | 2 : n & 1),
        t) : (Te(t),
        null);
    case 22:
    case 23:
        return Gu(),
        r = t.memoizedState !== null,
        e !== null && e.memoizedState !== null !== r && (t.flags |= 8192),
        r && t.mode & 1 ? Ke & 1073741824 && (Te(t),
        t.subtreeFlags & 6 && (t.flags |= 8192)) : Te(t),
        null;
    case 24:
        return null;
    case 25:
        return null
    }
    throw Error(A(156, t.tag))
}
function p0(e, t) {
    switch (Tu(t),
    t.tag) {
    case 1:
        return Be(t.type) && fs(),
        e = t.flags,
        e & 65536 ? (t.flags = e & -65537 | 128,
        t) : null;
    case 3:
        return Hr(),
        ie(Ue),
        ie(je),
        Du(),
        e = t.flags,
        e & 65536 && !(e & 128) ? (t.flags = e & -65537 | 128,
        t) : null;
    case 5:
        return Iu(t),
        null;
    case 13:
        if (ie(ue),
        e = t.memoizedState,
        e !== null && e.dehydrated !== null) {
            if (t.alternate === null)
                throw Error(A(340));
            Wr()
        }
        return e = t.flags,
        e & 65536 ? (t.flags = e & -65537 | 128,
        t) : null;
    case 19:
        return ie(ue),
        null;
    case 4:
        return Hr(),
        null;
    case 10:
        return Ou(t.type._context),
        null;
    case 22:
    case 23:
        return Gu(),
        null;
    case 24:
        return null;
    default:
        return null
    }
}
var _i = !1
  , Ae = !1
  , h0 = typeof WeakSet == "function" ? WeakSet : Set
  , M = null;
function Sr(e, t) {
    var n = e.ref;
    if (n !== null)
        if (typeof n == "function")
            try {
                n(null)
            } catch (r) {
                pe(e, t, r)
            }
        else
            n.current = null
}
function Ma(e, t, n) {
    try {
        n()
    } catch (r) {
        pe(e, t, r)
    }
}
var Ad = !1;
function m0(e, t) {
    if (ya = as,
    e = _p(),
    Pu(e)) {
        if ("selectionStart"in e)
            var n = {
                start: e.selectionStart,
                end: e.selectionEnd
            };
        else
            e: {
                n = (n = e.ownerDocument) && n.defaultView || window;
                var r = n.getSelection && n.getSelection();
                if (r && r.rangeCount !== 0) {
                    n = r.anchorNode;
                    var o = r.anchorOffset
                      , i = r.focusNode;
                    r = r.focusOffset;
                    try {
                        n.nodeType,
                        i.nodeType
                    } catch {
                        n = null;
                        break e
                    }
                    var s = 0
                      , l = -1
                      , a = -1
                      , u = 0
                      , d = 0
                      , f = e
                      , c = null;
                    t: for (; ; ) {
                        for (var y; f !== n || o !== 0 && f.nodeType !== 3 || (l = s + o),
                        f !== i || r !== 0 && f.nodeType !== 3 || (a = s + r),
                        f.nodeType === 3 && (s += f.nodeValue.length),
                        (y = f.firstChild) !== null; )
                            c = f,
                            f = y;
                        for (; ; ) {
                            if (f === e)
                                break t;
                            if (c === n && ++u === o && (l = s),
                            c === i && ++d === r && (a = s),
                            (y = f.nextSibling) !== null)
                                break;
                            f = c,
                            c = f.parentNode
                        }
                        f = y
                    }
                    n = l === -1 || a === -1 ? null : {
                        start: l,
                        end: a
                    }
                } else
                    n = null
            }
        n = n || {
            start: 0,
            end: 0
        }
    } else
        n = null;
    for (xa = {
        focusedElem: e,
        selectionRange: n
    },
    as = !1,
    M = t; M !== null; )
        if (t = M,
        e = t.child,
        (t.subtreeFlags & 1028) !== 0 && e !== null)
            e.return = t,
            M = e;
        else
            for (; M !== null; ) {
                t = M;
                try {
                    var w = t.alternate;
                    if (t.flags & 1024)
                        switch (t.tag) {
                        case 0:
                        case 11:
                        case 15:
                            break;
                        case 1:
                            if (w !== null) {
                                var x = w.memoizedProps
                                  , E = w.memoizedState
                                  , h = t.stateNode
                                  , p = h.getSnapshotBeforeUpdate(t.elementType === t.type ? x : dt(t.type, x), E);
                                h.__reactInternalSnapshotBeforeUpdate = p
                            }
                            break;
                        case 3:
                            var v = t.stateNode.containerInfo;
                            v.nodeType === 1 ? v.textContent = "" : v.nodeType === 9 && v.documentElement && v.removeChild(v.documentElement);
                            break;
                        case 5:
                        case 6:
                        case 4:
                        case 17:
                            break;
                        default:
                            throw Error(A(163))
                        }
                } catch (S) {
                    pe(t, t.return, S)
                }
                if (e = t.sibling,
                e !== null) {
                    e.return = t.return,
                    M = e;
                    break
                }
                M = t.return
            }
    return w = Ad,
    Ad = !1,
    w
}
function Ro(e, t, n) {
    var r = t.updateQueue;
    if (r = r !== null ? r.lastEffect : null,
    r !== null) {
        var o = r = r.next;
        do {
            if ((o.tag & e) === e) {
                var i = o.destroy;
                o.destroy = void 0,
                i !== void 0 && Ma(t, n, i)
            }
            o = o.next
        } while (o !== r)
    }
}
function Vs(e, t) {
    if (t = t.updateQueue,
    t = t !== null ? t.lastEffect : null,
    t !== null) {
        var n = t = t.next;
        do {
            if ((n.tag & e) === e) {
                var r = n.create;
                n.destroy = r()
            }
            n = n.next
        } while (n !== t)
    }
}
function Ia(e) {
    var t = e.ref;
    if (t !== null) {
        var n = e.stateNode;
        switch (e.tag) {
        case 5:
            e = n;
            break;
        default:
            e = n
        }
        typeof t == "function" ? t(e) : t.current = e
    }
}
function Rh(e) {
    var t = e.alternate;
    t !== null && (e.alternate = null,
    Rh(t)),
    e.child = null,
    e.deletions = null,
    e.sibling = null,
    e.tag === 5 && (t = e.stateNode,
    t !== null && (delete t[jt],
    delete t[Wo],
    delete t[Sa],
    delete t[qy],
    delete t[Zy])),
    e.stateNode = null,
    e.return = null,
    e.dependencies = null,
    e.memoizedProps = null,
    e.memoizedState = null,
    e.pendingProps = null,
    e.stateNode = null,
    e.updateQueue = null
}
function Ah(e) {
    return e.tag === 5 || e.tag === 3 || e.tag === 4
}
function jd(e) {
    e: for (; ; ) {
        for (; e.sibling === null; ) {
            if (e.return === null || Ah(e.return))
                return null;
            e = e.return
        }
        for (e.sibling.return = e.return,
        e = e.sibling; e.tag !== 5 && e.tag !== 6 && e.tag !== 18; ) {
            if (e.flags & 2 || e.child === null || e.tag === 4)
                continue e;
            e.child.return = e,
            e = e.child
        }
        if (!(e.flags & 2))
            return e.stateNode
    }
}
function Da(e, t, n) {
    var r = e.tag;
    if (r === 5 || r === 6)
        e = e.stateNode,
        t ? n.nodeType === 8 ? n.parentNode.insertBefore(e, t) : n.insertBefore(e, t) : (n.nodeType === 8 ? (t = n.parentNode,
        t.insertBefore(e, n)) : (t = n,
        t.appendChild(e)),
        n = n._reactRootContainer,
        n != null || t.onclick !== null || (t.onclick = ds));
    else if (r !== 4 && (e = e.child,
    e !== null))
        for (Da(e, t, n),
        e = e.sibling; e !== null; )
            Da(e, t, n),
            e = e.sibling
}
function za(e, t, n) {
    var r = e.tag;
    if (r === 5 || r === 6)
        e = e.stateNode,
        t ? n.insertBefore(e, t) : n.appendChild(e);
    else if (r !== 4 && (e = e.child,
    e !== null))
        for (za(e, t, n),
        e = e.sibling; e !== null; )
            za(e, t, n),
            e = e.sibling
}
var Ce = null
  , ht = !1;
function nn(e, t, n) {
    for (n = n.child; n !== null; )
        jh(e, t, n),
        n = n.sibling
}
function jh(e, t, n) {
    if (Ot && typeof Ot.onCommitFiberUnmount == "function")
        try {
            Ot.onCommitFiberUnmount(Is, n)
        } catch {}
    switch (n.tag) {
    case 5:
        Ae || Sr(n, t);
    case 6:
        var r = Ce
          , o = ht;
        Ce = null,
        nn(e, t, n),
        Ce = r,
        ht = o,
        Ce !== null && (ht ? (e = Ce,
        n = n.stateNode,
        e.nodeType === 8 ? e.parentNode.removeChild(n) : e.removeChild(n)) : Ce.removeChild(n.stateNode));
        break;
    case 18:
        Ce !== null && (ht ? (e = Ce,
        n = n.stateNode,
        e.nodeType === 8 ? _l(e.parentNode, n) : e.nodeType === 1 && _l(e, n),
        zo(e)) : _l(Ce, n.stateNode));
        break;
    case 4:
        r = Ce,
        o = ht,
        Ce = n.stateNode.containerInfo,
        ht = !0,
        nn(e, t, n),
        Ce = r,
        ht = o;
        break;
    case 0:
    case 11:
    case 14:
    case 15:
        if (!Ae && (r = n.updateQueue,
        r !== null && (r = r.lastEffect,
        r !== null))) {
            o = r = r.next;
            do {
                var i = o
                  , s = i.destroy;
                i = i.tag,
                s !== void 0 && (i & 2 || i & 4) && Ma(n, t, s),
                o = o.next
            } while (o !== r)
        }
        nn(e, t, n);
        break;
    case 1:
        if (!Ae && (Sr(n, t),
        r = n.stateNode,
        typeof r.componentWillUnmount == "function"))
            try {
                r.props = n.memoizedProps,
                r.state = n.memoizedState,
                r.componentWillUnmount()
            } catch (l) {
                pe(n, t, l)
            }
        nn(e, t, n);
        break;
    case 21:
        nn(e, t, n);
        break;
    case 22:
        n.mode & 1 ? (Ae = (r = Ae) || n.memoizedState !== null,
        nn(e, t, n),
        Ae = r) : nn(e, t, n);
        break;
    default:
        nn(e, t, n)
    }
}
function Od(e) {
    var t = e.updateQueue;
    if (t !== null) {
        e.updateQueue = null;
        var n = e.stateNode;
        n === null && (n = e.stateNode = new h0),
        t.forEach(function(r) {
            var o = b0.bind(null, e, r);
            n.has(r) || (n.add(r),
            r.then(o, o))
        })
    }
}
function ct(e, t) {
    var n = t.deletions;
    if (n !== null)
        for (var r = 0; r < n.length; r++) {
            var o = n[r];
            try {
                var i = e
                  , s = t
                  , l = s;
                e: for (; l !== null; ) {
                    switch (l.tag) {
                    case 5:
                        Ce = l.stateNode,
                        ht = !1;
                        break e;
                    case 3:
                        Ce = l.stateNode.containerInfo,
                        ht = !0;
                        break e;
                    case 4:
                        Ce = l.stateNode.containerInfo,
                        ht = !0;
                        break e
                    }
                    l = l.return
                }
                if (Ce === null)
                    throw Error(A(160));
                jh(i, s, o),
                Ce = null,
                ht = !1;
                var a = o.alternate;
                a !== null && (a.return = null),
                o.return = null
            } catch (u) {
                pe(o, t, u)
            }
        }
    if (t.subtreeFlags & 12854)
        for (t = t.child; t !== null; )
            Oh(t, e),
            t = t.sibling
}
function Oh(e, t) {
    var n = e.alternate
      , r = e.flags;
    switch (e.tag) {
    case 0:
    case 11:
    case 14:
    case 15:
        if (ct(t, e),
        Pt(e),
        r & 4) {
            try {
                Ro(3, e, e.return),
                Vs(3, e)
            } catch (x) {
                pe(e, e.return, x)
            }
            try {
                Ro(5, e, e.return)
            } catch (x) {
                pe(e, e.return, x)
            }
        }
        break;
    case 1:
        ct(t, e),
        Pt(e),
        r & 512 && n !== null && Sr(n, n.return);
        break;
    case 5:
        if (ct(t, e),
        Pt(e),
        r & 512 && n !== null && Sr(n, n.return),
        e.flags & 32) {
            var o = e.stateNode;
            try {
                Lo(o, "")
            } catch (x) {
                pe(e, e.return, x)
            }
        }
        if (r & 4 && (o = e.stateNode,
        o != null)) {
            var i = e.memoizedProps
              , s = n !== null ? n.memoizedProps : i
              , l = e.type
              , a = e.updateQueue;
            if (e.updateQueue = null,
            a !== null)
                try {
                    l === "input" && i.type === "radio" && i.name != null && ep(o, i),
                    aa(l, s);
                    var u = aa(l, i);
                    for (s = 0; s < a.length; s += 2) {
                        var d = a[s]
                          , f = a[s + 1];
                        d === "style" ? ip(o, f) : d === "dangerouslySetInnerHTML" ? rp(o, f) : d === "children" ? Lo(o, f) : pu(o, d, f, u)
                    }
                    switch (l) {
                    case "input":
                        ra(o, i);
                        break;
                    case "textarea":
                        tp(o, i);
                        break;
                    case "select":
                        var c = o._wrapperState.wasMultiple;
                        o._wrapperState.wasMultiple = !!i.multiple;
                        var y = i.value;
                        y != null ? br(o, !!i.multiple, y, !1) : c !== !!i.multiple && (i.defaultValue != null ? br(o, !!i.multiple, i.defaultValue, !0) : br(o, !!i.multiple, i.multiple ? [] : "", !1))
                    }
                    o[Wo] = i
                } catch (x) {
                    pe(e, e.return, x)
                }
        }
        break;
    case 6:
        if (ct(t, e),
        Pt(e),
        r & 4) {
            if (e.stateNode === null)
                throw Error(A(162));
            o = e.stateNode,
            i = e.memoizedProps;
            try {
                o.nodeValue = i
            } catch (x) {
                pe(e, e.return, x)
            }
        }
        break;
    case 3:
        if (ct(t, e),
        Pt(e),
        r & 4 && n !== null && n.memoizedState.isDehydrated)
            try {
                zo(t.containerInfo)
            } catch (x) {
                pe(e, e.return, x)
            }
        break;
    case 4:
        ct(t, e),
        Pt(e);
        break;
    case 13:
        ct(t, e),
        Pt(e),
        o = e.child,
        o.flags & 8192 && (i = o.memoizedState !== null,
        o.stateNode.isHidden = i,
        !i || o.alternate !== null && o.alternate.memoizedState !== null || (Qu = he())),
        r & 4 && Od(e);
        break;
    case 22:
        if (d = n !== null && n.memoizedState !== null,
        e.mode & 1 ? (Ae = (u = Ae) || d,
        ct(t, e),
        Ae = u) : ct(t, e),
        Pt(e),
        r & 8192) {
            if (u = e.memoizedState !== null,
            (e.stateNode.isHidden = u) && !d && e.mode & 1)
                for (M = e,
                d = e.child; d !== null; ) {
                    for (f = M = d; M !== null; ) {
                        switch (c = M,
                        y = c.child,
                        c.tag) {
                        case 0:
                        case 11:
                        case 14:
                        case 15:
                            Ro(4, c, c.return);
                            break;
                        case 1:
                            Sr(c, c.return);
                            var w = c.stateNode;
                            if (typeof w.componentWillUnmount == "function") {
                                r = c,
                                n = c.return;
                                try {
                                    t = r,
                                    w.props = t.memoizedProps,
                                    w.state = t.memoizedState,
                                    w.componentWillUnmount()
                                } catch (x) {
                                    pe(r, n, x)
                                }
                            }
                            break;
                        case 5:
                            Sr(c, c.return);
                            break;
                        case 22:
                            if (c.memoizedState !== null) {
                                Ld(f);
                                continue
                            }
                        }
                        y !== null ? (y.return = c,
                        M = y) : Ld(f)
                    }
                    d = d.sibling
                }
            e: for (d = null,
            f = e; ; ) {
                if (f.tag === 5) {
                    if (d === null) {
                        d = f;
                        try {
                            o = f.stateNode,
                            u ? (i = o.style,
                            typeof i.setProperty == "function" ? i.setProperty("display", "none", "important") : i.display = "none") : (l = f.stateNode,
                            a = f.memoizedProps.style,
                            s = a != null && a.hasOwnProperty("display") ? a.display : null,
                            l.style.display = op("display", s))
                        } catch (x) {
                            pe(e, e.return, x)
                        }
                    }
                } else if (f.tag === 6) {
                    if (d === null)
                        try {
                            f.stateNode.nodeValue = u ? "" : f.memoizedProps
                        } catch (x) {
                            pe(e, e.return, x)
                        }
                } else if ((f.tag !== 22 && f.tag !== 23 || f.memoizedState === null || f === e) && f.child !== null) {
                    f.child.return = f,
                    f = f.child;
                    continue
                }
                if (f === e)
                    break e;
                for (; f.sibling === null; ) {
                    if (f.return === null || f.return === e)
                        break e;
                    d === f && (d = null),
                    f = f.return
                }
                d === f && (d = null),
                f.sibling.return = f.return,
                f = f.sibling
            }
        }
        break;
    case 19:
        ct(t, e),
        Pt(e),
        r & 4 && Od(e);
        break;
    case 21:
        break;
    default:
        ct(t, e),
        Pt(e)
    }
}
function Pt(e) {
    var t = e.flags;
    if (t & 2) {
        try {
            e: {
                for (var n = e.return; n !== null; ) {
                    if (Ah(n)) {
                        var r = n;
                        break e
                    }
                    n = n.return
                }
                throw Error(A(160))
            }
            switch (r.tag) {
            case 5:
                var o = r.stateNode;
                r.flags & 32 && (Lo(o, ""),
                r.flags &= -33);
                var i = jd(e);
                za(e, i, o);
                break;
            case 3:
            case 4:
                var s = r.stateNode.containerInfo
                  , l = jd(e);
                Da(e, l, s);
                break;
            default:
                throw Error(A(161))
            }
        } catch (a) {
            pe(e, e.return, a)
        }
        e.flags &= -3
    }
    t & 4096 && (e.flags &= -4097)
}
function v0(e, t, n) {
    M = e,
    _h(e)
}
function _h(e, t, n) {
    for (var r = (e.mode & 1) !== 0; M !== null; ) {
        var o = M
          , i = o.child;
        if (o.tag === 22 && r) {
            var s = o.memoizedState !== null || _i;
            if (!s) {
                var l = o.alternate
                  , a = l !== null && l.memoizedState !== null || Ae;
                l = _i;
                var u = Ae;
                if (_i = s,
                (Ae = a) && !u)
                    for (M = o; M !== null; )
                        s = M,
                        a = s.child,
                        s.tag === 22 && s.memoizedState !== null ? Md(o) : a !== null ? (a.return = s,
                        M = a) : Md(o);
                for (; i !== null; )
                    M = i,
                    _h(i),
                    i = i.sibling;
                M = o,
                _i = l,
                Ae = u
            }
            _d(e)
        } else
            o.subtreeFlags & 8772 && i !== null ? (i.return = o,
            M = i) : _d(e)
    }
}
function _d(e) {
    for (; M !== null; ) {
        var t = M;
        if (t.flags & 8772) {
            var n = t.alternate;
            try {
                if (t.flags & 8772)
                    switch (t.tag) {
                    case 0:
                    case 11:
                    case 15:
                        Ae || Vs(5, t);
                        break;
                    case 1:
                        var r = t.stateNode;
                        if (t.flags & 4 && !Ae)
                            if (n === null)
                                r.componentDidMount();
                            else {
                                var o = t.elementType === t.type ? n.memoizedProps : dt(t.type, n.memoizedProps);
                                r.componentDidUpdate(o, n.memoizedState, r.__reactInternalSnapshotBeforeUpdate)
                            }
                        var i = t.updateQueue;
                        i !== null && gd(t, i, r);
                        break;
                    case 3:
                        var s = t.updateQueue;
                        if (s !== null) {
                            if (n = null,
                            t.child !== null)
                                switch (t.child.tag) {
                                case 5:
                                    n = t.child.stateNode;
                                    break;
                                case 1:
                                    n = t.child.stateNode
                                }
                            gd(t, s, n)
                        }
                        break;
                    case 5:
                        var l = t.stateNode;
                        if (n === null && t.flags & 4) {
                            n = l;
                            var a = t.memoizedProps;
                            switch (t.type) {
                            case "button":
                            case "input":
                            case "select":
                            case "textarea":
                                a.autoFocus && n.focus();
                                break;
                            case "img":
                                a.src && (n.src = a.src)
                            }
                        }
                        break;
                    case 6:
                        break;
                    case 4:
                        break;
                    case 12:
                        break;
                    case 13:
                        if (t.memoizedState === null) {
                            var u = t.alternate;
                            if (u !== null) {
                                var d = u.memoizedState;
                                if (d !== null) {
                                    var f = d.dehydrated;
                                    f !== null && zo(f)
                                }
                            }
                        }
                        break;
                    case 19:
                    case 17:
                    case 21:
                    case 22:
                    case 23:
                    case 25:
                        break;
                    default:
                        throw Error(A(163))
                    }
                Ae || t.flags & 512 && Ia(t)
            } catch (c) {
                pe(t, t.return, c)
            }
        }
        if (t === e) {
            M = null;
            break
        }
        if (n = t.sibling,
        n !== null) {
            n.return = t.return,
            M = n;
            break
        }
        M = t.return
    }
}
function Ld(e) {
    for (; M !== null; ) {
        var t = M;
        if (t === e) {
            M = null;
            break
        }
        var n = t.sibling;
        if (n !== null) {
            n.return = t.return,
            M = n;
            break
        }
        M = t.return
    }
}
function Md(e) {
    for (; M !== null; ) {
        var t = M;
        try {
            switch (t.tag) {
            case 0:
            case 11:
            case 15:
                var n = t.return;
                try {
                    Vs(4, t)
                } catch (a) {
                    pe(t, n, a)
                }
                break;
            case 1:
                var r = t.stateNode;
                if (typeof r.componentDidMount == "function") {
                    var o = t.return;
                    try {
                        r.componentDidMount()
                    } catch (a) {
                        pe(t, o, a)
                    }
                }
                var i = t.return;
                try {
                    Ia(t)
                } catch (a) {
                    pe(t, i, a)
                }
                break;
            case 5:
                var s = t.return;
                try {
                    Ia(t)
                } catch (a) {
                    pe(t, s, a)
                }
            }
        } catch (a) {
            pe(t, t.return, a)
        }
        if (t === e) {
            M = null;
            break
        }
        var l = t.sibling;
        if (l !== null) {
            l.return = t.return,
            M = l;
            break
        }
        M = t.return
    }
}
var g0 = Math.ceil
  , Ss = Yt.ReactCurrentDispatcher
  , Vu = Yt.ReactCurrentOwner
  , it = Yt.ReactCurrentBatchConfig
  , X = 0
  , Ee = null
  , ve = null
  , be = 0
  , Ke = 0
  , Cr = Mn(0)
  , xe = 0
  , Yo = null
  , er = 0
  , Hs = 0
  , Hu = 0
  , Ao = null
  , Fe = null
  , Qu = 0
  , Kr = 1 / 0
  , zt = null
  , Cs = !1
  , Fa = null
  , bn = null
  , Li = !1
  , gn = null
  , bs = 0
  , jo = 0
  , $a = null
  , qi = -1
  , Zi = 0;
function Ie() {
    return X & 6 ? he() : qi !== -1 ? qi : qi = he()
}
function kn(e) {
    return e.mode & 1 ? X & 2 && be !== 0 ? be & -be : e0.transition !== null ? (Zi === 0 && (Zi = gp()),
    Zi) : (e = J,
    e !== 0 || (e = window.event,
    e = e === void 0 ? 16 : bp(e.type)),
    e) : 1
}
function gt(e, t, n, r) {
    if (50 < jo)
        throw jo = 0,
        $a = null,
        Error(A(185));
    si(e, n, r),
    (!(X & 2) || e !== Ee) && (e === Ee && (!(X & 2) && (Hs |= n),
    xe === 4 && cn(e, be)),
    We(e, r),
    n === 1 && X === 0 && !(t.mode & 1) && (Kr = he() + 500,
    Us && In()))
}
function We(e, t) {
    var n = e.callbackNode;
    ey(e, t);
    var r = ls(e, e === Ee ? be : 0);
    if (r === 0)
        n !== null && Vc(n),
        e.callbackNode = null,
        e.callbackPriority = 0;
    else if (t = r & -r,
    e.callbackPriority !== t) {
        if (n != null && Vc(n),
        t === 1)
            e.tag === 0 ? Jy(Id.bind(null, e)) : Wp(Id.bind(null, e)),
            Yy(function() {
                !(X & 6) && In()
            }),
            n = null;
        else {
            switch (yp(r)) {
            case 1:
                n = yu;
                break;
            case 4:
                n = mp;
                break;
            case 16:
                n = ss;
                break;
            case 536870912:
                n = vp;
                break;
            default:
                n = ss
            }
            n = Uh(n, Lh.bind(null, e))
        }
        e.callbackPriority = t,
        e.callbackNode = n
    }
}
function Lh(e, t) {
    if (qi = -1,
    Zi = 0,
    X & 6)
        throw Error(A(327));
    var n = e.callbackNode;
    if (Rr() && e.callbackNode !== n)
        return null;
    var r = ls(e, e === Ee ? be : 0);
    if (r === 0)
        return null;
    if (r & 30 || r & e.expiredLanes || t)
        t = ks(e, r);
    else {
        t = r;
        var o = X;
        X |= 2;
        var i = Ih();
        (Ee !== e || be !== t) && (zt = null,
        Kr = he() + 500,
        Yn(e, t));
        do
            try {
                w0();
                break
            } catch (l) {
                Mh(e, l)
            }
        while (!0);
        ju(),
        Ss.current = i,
        X = o,
        ve !== null ? t = 0 : (Ee = null,
        be = 0,
        t = xe)
    }
    if (t !== 0) {
        if (t === 2 && (o = pa(e),
        o !== 0 && (r = o,
        t = Ua(e, o))),
        t === 1)
            throw n = Yo,
            Yn(e, 0),
            cn(e, r),
            We(e, he()),
            n;
        if (t === 6)
            cn(e, r);
        else {
            if (o = e.current.alternate,
            !(r & 30) && !y0(o) && (t = ks(e, r),
            t === 2 && (i = pa(e),
            i !== 0 && (r = i,
            t = Ua(e, i))),
            t === 1))
                throw n = Yo,
                Yn(e, 0),
                cn(e, r),
                We(e, he()),
                n;
            switch (e.finishedWork = o,
            e.finishedLanes = r,
            t) {
            case 0:
            case 1:
                throw Error(A(345));
            case 2:
                Fn(e, Fe, zt);
                break;
            case 3:
                if (cn(e, r),
                (r & 130023424) === r && (t = Qu + 500 - he(),
                10 < t)) {
                    if (ls(e, 0) !== 0)
                        break;
                    if (o = e.suspendedLanes,
                    (o & r) !== r) {
                        Ie(),
                        e.pingedLanes |= e.suspendedLanes & o;
                        break
                    }
                    e.timeoutHandle = Ea(Fn.bind(null, e, Fe, zt), t);
                    break
                }
                Fn(e, Fe, zt);
                break;
            case 4:
                if (cn(e, r),
                (r & 4194240) === r)
                    break;
                for (t = e.eventTimes,
                o = -1; 0 < r; ) {
                    var s = 31 - vt(r);
                    i = 1 << s,
                    s = t[s],
                    s > o && (o = s),
                    r &= ~i
                }
                if (r = o,
                r = he() - r,
                r = (120 > r ? 120 : 480 > r ? 480 : 1080 > r ? 1080 : 1920 > r ? 1920 : 3e3 > r ? 3e3 : 4320 > r ? 4320 : 1960 * g0(r / 1960)) - r,
                10 < r) {
                    e.timeoutHandle = Ea(Fn.bind(null, e, Fe, zt), r);
                    break
                }
                Fn(e, Fe, zt);
                break;
            case 5:
                Fn(e, Fe, zt);
                break;
            default:
                throw Error(A(329))
            }
        }
    }
    return We(e, he()),
    e.callbackNode === n ? Lh.bind(null, e) : null
}
function Ua(e, t) {
    var n = Ao;
    return e.current.memoizedState.isDehydrated && (Yn(e, t).flags |= 256),
    e = ks(e, t),
    e !== 2 && (t = Fe,
    Fe = n,
    t !== null && Ba(t)),
    e
}
function Ba(e) {
    Fe === null ? Fe = e : Fe.push.apply(Fe, e)
}
function y0(e) {
    for (var t = e; ; ) {
        if (t.flags & 16384) {
            var n = t.updateQueue;
            if (n !== null && (n = n.stores,
            n !== null))
                for (var r = 0; r < n.length; r++) {
                    var o = n[r]
                      , i = o.getSnapshot;
                    o = o.value;
                    try {
                        if (!yt(i(), o))
                            return !1
                    } catch {
                        return !1
                    }
                }
        }
        if (n = t.child,
        t.subtreeFlags & 16384 && n !== null)
            n.return = t,
            t = n;
        else {
            if (t === e)
                break;
            for (; t.sibling === null; ) {
                if (t.return === null || t.return === e)
                    return !0;
                t = t.return
            }
            t.sibling.return = t.return,
            t = t.sibling
        }
    }
    return !0
}
function cn(e, t) {
    for (t &= ~Hu,
    t &= ~Hs,
    e.suspendedLanes |= t,
    e.pingedLanes &= ~t,
    e = e.expirationTimes; 0 < t; ) {
        var n = 31 - vt(t)
          , r = 1 << n;
        e[n] = -1,
        t &= ~r
    }
}
function Id(e) {
    if (X & 6)
        throw Error(A(327));
    Rr();
    var t = ls(e, 0);
    if (!(t & 1))
        return We(e, he()),
        null;
    var n = ks(e, t);
    if (e.tag !== 0 && n === 2) {
        var r = pa(e);
        r !== 0 && (t = r,
        n = Ua(e, r))
    }
    if (n === 1)
        throw n = Yo,
        Yn(e, 0),
        cn(e, t),
        We(e, he()),
        n;
    if (n === 6)
        throw Error(A(345));
    return e.finishedWork = e.current.alternate,
    e.finishedLanes = t,
    Fn(e, Fe, zt),
    We(e, he()),
    null
}
function Ku(e, t) {
    var n = X;
    X |= 1;
    try {
        return e(t)
    } finally {
        X = n,
        X === 0 && (Kr = he() + 500,
        Us && In())
    }
}
function tr(e) {
    gn !== null && gn.tag === 0 && !(X & 6) && Rr();
    var t = X;
    X |= 1;
    var n = it.transition
      , r = J;
    try {
        if (it.transition = null,
        J = 1,
        e)
            return e()
    } finally {
        J = r,
        it.transition = n,
        X = t,
        !(X & 6) && In()
    }
}
function Gu() {
    Ke = Cr.current,
    ie(Cr)
}
function Yn(e, t) {
    e.finishedWork = null,
    e.finishedLanes = 0;
    var n = e.timeoutHandle;
    if (n !== -1 && (e.timeoutHandle = -1,
    Gy(n)),
    ve !== null)
        for (n = ve.return; n !== null; ) {
            var r = n;
            switch (Tu(r),
            r.tag) {
            case 1:
                r = r.type.childContextTypes,
                r != null && fs();
                break;
            case 3:
                Hr(),
                ie(Ue),
                ie(je),
                Du();
                break;
            case 5:
                Iu(r);
                break;
            case 4:
                Hr();
                break;
            case 13:
                ie(ue);
                break;
            case 19:
                ie(ue);
                break;
            case 10:
                Ou(r.type._context);
                break;
            case 22:
            case 23:
                Gu()
            }
            n = n.return
        }
    if (Ee = e,
    ve = e = Pn(e.current, null),
    be = Ke = t,
    xe = 0,
    Yo = null,
    Hu = Hs = er = 0,
    Fe = Ao = null,
    Wn !== null) {
        for (t = 0; t < Wn.length; t++)
            if (n = Wn[t],
            r = n.interleaved,
            r !== null) {
                n.interleaved = null;
                var o = r.next
                  , i = n.pending;
                if (i !== null) {
                    var s = i.next;
                    i.next = o,
                    r.next = s
                }
                n.pending = r
            }
        Wn = null
    }
    return e
}
function Mh(e, t) {
    do {
        var n = ve;
        try {
            if (ju(),
            Gi.current = Es,
            ws) {
                for (var r = ce.memoizedState; r !== null; ) {
                    var o = r.queue;
                    o !== null && (o.pending = null),
                    r = r.next
                }
                ws = !1
            }
            if (Jn = 0,
            we = ye = ce = null,
            To = !1,
            Qo = 0,
            Vu.current = null,
            n === null || n.return === null) {
                xe = 1,
                Yo = t,
                ve = null;
                break
            }
            e: {
                var i = e
                  , s = n.return
                  , l = n
                  , a = t;
                if (t = be,
                l.flags |= 32768,
                a !== null && typeof a == "object" && typeof a.then == "function") {
                    var u = a
                      , d = l
                      , f = d.tag;
                    if (!(d.mode & 1) && (f === 0 || f === 11 || f === 15)) {
                        var c = d.alternate;
                        c ? (d.updateQueue = c.updateQueue,
                        d.memoizedState = c.memoizedState,
                        d.lanes = c.lanes) : (d.updateQueue = null,
                        d.memoizedState = null)
                    }
                    var y = Cd(s);
                    if (y !== null) {
                        y.flags &= -257,
                        bd(y, s, l, i, t),
                        y.mode & 1 && Sd(i, u, t),
                        t = y,
                        a = u;
                        var w = t.updateQueue;
                        if (w === null) {
                            var x = new Set;
                            x.add(a),
                            t.updateQueue = x
                        } else
                            w.add(a);
                        break e
                    } else {
                        if (!(t & 1)) {
                            Sd(i, u, t),
                            Yu();
                            break e
                        }
                        a = Error(A(426))
                    }
                } else if (se && l.mode & 1) {
                    var E = Cd(s);
                    if (E !== null) {
                        !(E.flags & 65536) && (E.flags |= 256),
                        bd(E, s, l, i, t),
                        Ru(Qr(a, l));
                        break e
                    }
                }
                i = a = Qr(a, l),
                xe !== 4 && (xe = 2),
                Ao === null ? Ao = [i] : Ao.push(i),
                i = s;
                do {
                    switch (i.tag) {
                    case 3:
                        i.flags |= 65536,
                        t &= -t,
                        i.lanes |= t;
                        var h = yh(i, a, t);
                        vd(i, h);
                        break e;
                    case 1:
                        l = a;
                        var p = i.type
                          , v = i.stateNode;
                        if (!(i.flags & 128) && (typeof p.getDerivedStateFromError == "function" || v !== null && typeof v.componentDidCatch == "function" && (bn === null || !bn.has(v)))) {
                            i.flags |= 65536,
                            t &= -t,
                            i.lanes |= t;
                            var S = xh(i, l, t);
                            vd(i, S);
                            break e
                        }
                    }
                    i = i.return
                } while (i !== null)
            }
            zh(n)
        } catch (C) {
            t = C,
            ve === n && n !== null && (ve = n = n.return);
            continue
        }
        break
    } while (!0)
}
function Ih() {
    var e = Ss.current;
    return Ss.current = Es,
    e === null ? Es : e
}
function Yu() {
    (xe === 0 || xe === 3 || xe === 2) && (xe = 4),
    Ee === null || !(er & 268435455) && !(Hs & 268435455) || cn(Ee, be)
}
function ks(e, t) {
    var n = X;
    X |= 2;
    var r = Ih();
    (Ee !== e || be !== t) && (zt = null,
    Yn(e, t));
    do
        try {
            x0();
            break
        } catch (o) {
            Mh(e, o)
        }
    while (!0);
    if (ju(),
    X = n,
    Ss.current = r,
    ve !== null)
        throw Error(A(261));
    return Ee = null,
    be = 0,
    xe
}
function x0() {
    for (; ve !== null; )
        Dh(ve)
}
function w0() {
    for (; ve !== null && !Hg(); )
        Dh(ve)
}
function Dh(e) {
    var t = $h(e.alternate, e, Ke);
    e.memoizedProps = e.pendingProps,
    t === null ? zh(e) : ve = t,
    Vu.current = null
}
function zh(e) {
    var t = e;
    do {
        var n = t.alternate;
        if (e = t.return,
        t.flags & 32768) {
            if (n = p0(n, t),
            n !== null) {
                n.flags &= 32767,
                ve = n;
                return
            }
            if (e !== null)
                e.flags |= 32768,
                e.subtreeFlags = 0,
                e.deletions = null;
            else {
                xe = 6,
                ve = null;
                return
            }
        } else if (n = f0(n, t, Ke),
        n !== null) {
            ve = n;
            return
        }
        if (t = t.sibling,
        t !== null) {
            ve = t;
            return
        }
        ve = t = e
    } while (t !== null);
    xe === 0 && (xe = 5)
}
function Fn(e, t, n) {
    var r = J
      , o = it.transition;
    try {
        it.transition = null,
        J = 1,
        E0(e, t, n, r)
    } finally {
        it.transition = o,
        J = r
    }
    return null
}
function E0(e, t, n, r) {
    do
        Rr();
    while (gn !== null);
    if (X & 6)
        throw Error(A(327));
    n = e.finishedWork;
    var o = e.finishedLanes;
    if (n === null)
        return null;
    if (e.finishedWork = null,
    e.finishedLanes = 0,
    n === e.current)
        throw Error(A(177));
    e.callbackNode = null,
    e.callbackPriority = 0;
    var i = n.lanes | n.childLanes;
    if (ty(e, i),
    e === Ee && (ve = Ee = null,
    be = 0),
    !(n.subtreeFlags & 2064) && !(n.flags & 2064) || Li || (Li = !0,
    Uh(ss, function() {
        return Rr(),
        null
    })),
    i = (n.flags & 15990) !== 0,
    n.subtreeFlags & 15990 || i) {
        i = it.transition,
        it.transition = null;
        var s = J;
        J = 1;
        var l = X;
        X |= 4,
        Vu.current = null,
        m0(e, n),
        Oh(n, e),
        Uy(xa),
        as = !!ya,
        xa = ya = null,
        e.current = n,
        v0(n),
        Qg(),
        X = l,
        J = s,
        it.transition = i
    } else
        e.current = n;
    if (Li && (Li = !1,
    gn = e,
    bs = o),
    i = e.pendingLanes,
    i === 0 && (bn = null),
    Yg(n.stateNode),
    We(e, he()),
    t !== null)
        for (r = e.onRecoverableError,
        n = 0; n < t.length; n++)
            o = t[n],
            r(o.value, {
                componentStack: o.stack,
                digest: o.digest
            });
    if (Cs)
        throw Cs = !1,
        e = Fa,
        Fa = null,
        e;
    return bs & 1 && e.tag !== 0 && Rr(),
    i = e.pendingLanes,
    i & 1 ? e === $a ? jo++ : (jo = 0,
    $a = e) : jo = 0,
    In(),
    null
}
function Rr() {
    if (gn !== null) {
        var e = yp(bs)
          , t = it.transition
          , n = J;
        try {
            if (it.transition = null,
            J = 16 > e ? 16 : e,
            gn === null)
                var r = !1;
            else {
                if (e = gn,
                gn = null,
                bs = 0,
                X & 6)
                    throw Error(A(331));
                var o = X;
                for (X |= 4,
                M = e.current; M !== null; ) {
                    var i = M
                      , s = i.child;
                    if (M.flags & 16) {
                        var l = i.deletions;
                        if (l !== null) {
                            for (var a = 0; a < l.length; a++) {
                                var u = l[a];
                                for (M = u; M !== null; ) {
                                    var d = M;
                                    switch (d.tag) {
                                    case 0:
                                    case 11:
                                    case 15:
                                        Ro(8, d, i)
                                    }
                                    var f = d.child;
                                    if (f !== null)
                                        f.return = d,
                                        M = f;
                                    else
                                        for (; M !== null; ) {
                                            d = M;
                                            var c = d.sibling
                                              , y = d.return;
                                            if (Rh(d),
                                            d === u) {
                                                M = null;
                                                break
                                            }
                                            if (c !== null) {
                                                c.return = y,
                                                M = c;
                                                break
                                            }
                                            M = y
                                        }
                                }
                            }
                            var w = i.alternate;
                            if (w !== null) {
                                var x = w.child;
                                if (x !== null) {
                                    w.child = null;
                                    do {
                                        var E = x.sibling;
                                        x.sibling = null,
                                        x = E
                                    } while (x !== null)
                                }
                            }
                            M = i
                        }
                    }
                    if (i.subtreeFlags & 2064 && s !== null)
                        s.return = i,
                        M = s;
                    else
                        e: for (; M !== null; ) {
                            if (i = M,
                            i.flags & 2048)
                                switch (i.tag) {
                                case 0:
                                case 11:
                                case 15:
                                    Ro(9, i, i.return)
                                }
                            var h = i.sibling;
                            if (h !== null) {
                                h.return = i.return,
                                M = h;
                                break e
                            }
                            M = i.return
                        }
                }
                var p = e.current;
                for (M = p; M !== null; ) {
                    s = M;
                    var v = s.child;
                    if (s.subtreeFlags & 2064 && v !== null)
                        v.return = s,
                        M = v;
                    else
                        e: for (s = p; M !== null; ) {
                            if (l = M,
                            l.flags & 2048)
                                try {
                                    switch (l.tag) {
                                    case 0:
                                    case 11:
                                    case 15:
                                        Vs(9, l)
                                    }
                                } catch (C) {
                                    pe(l, l.return, C)
                                }
                            if (l === s) {
                                M = null;
                                break e
                            }
                            var S = l.sibling;
                            if (S !== null) {
                                S.return = l.return,
                                M = S;
                                break e
                            }
                            M = l.return
                        }
                }
                if (X = o,
                In(),
                Ot && typeof Ot.onPostCommitFiberRoot == "function")
                    try {
                        Ot.onPostCommitFiberRoot(Is, e)
                    } catch {}
                r = !0
            }
            return r
        } finally {
            J = n,
            it.transition = t
        }
    }
    return !1
}
function Dd(e, t, n) {
    t = Qr(n, t),
    t = yh(e, t, 1),
    e = Cn(e, t, 1),
    t = Ie(),
    e !== null && (si(e, 1, t),
    We(e, t))
}
function pe(e, t, n) {
    if (e.tag === 3)
        Dd(e, e, n);
    else
        for (; t !== null; ) {
            if (t.tag === 3) {
                Dd(t, e, n);
                break
            } else if (t.tag === 1) {
                var r = t.stateNode;
                if (typeof t.type.getDerivedStateFromError == "function" || typeof r.componentDidCatch == "function" && (bn === null || !bn.has(r))) {
                    e = Qr(n, e),
                    e = xh(t, e, 1),
                    t = Cn(t, e, 1),
                    e = Ie(),
                    t !== null && (si(t, 1, e),
                    We(t, e));
                    break
                }
            }
            t = t.return
        }
}
function S0(e, t, n) {
    var r = e.pingCache;
    r !== null && r.delete(t),
    t = Ie(),
    e.pingedLanes |= e.suspendedLanes & n,
    Ee === e && (be & n) === n && (xe === 4 || xe === 3 && (be & 130023424) === be && 500 > he() - Qu ? Yn(e, 0) : Hu |= n),
    We(e, t)
}
function Fh(e, t) {
    t === 0 && (e.mode & 1 ? (t = bi,
    bi <<= 1,
    !(bi & 130023424) && (bi = 4194304)) : t = 1);
    var n = Ie();
    e = Ht(e, t),
    e !== null && (si(e, t, n),
    We(e, n))
}
function C0(e) {
    var t = e.memoizedState
      , n = 0;
    t !== null && (n = t.retryLane),
    Fh(e, n)
}
function b0(e, t) {
    var n = 0;
    switch (e.tag) {
    case 13:
        var r = e.stateNode
          , o = e.memoizedState;
        o !== null && (n = o.retryLane);
        break;
    case 19:
        r = e.stateNode;
        break;
    default:
        throw Error(A(314))
    }
    r !== null && r.delete(t),
    Fh(e, n)
}
var $h;
$h = function(e, t, n) {
    if (e !== null)
        if (e.memoizedProps !== t.pendingProps || Ue.current)
            $e = !0;
        else {
            if (!(e.lanes & n) && !(t.flags & 128))
                return $e = !1,
                d0(e, t, n);
            $e = !!(e.flags & 131072)
        }
    else
        $e = !1,
        se && t.flags & 1048576 && Vp(t, ms, t.index);
    switch (t.lanes = 0,
    t.tag) {
    case 2:
        var r = t.type;
        Xi(e, t),
        e = t.pendingProps;
        var o = Br(t, je.current);
        Tr(t, n),
        o = Fu(null, t, r, e, o, n);
        var i = $u();
        return t.flags |= 1,
        typeof o == "object" && o !== null && typeof o.render == "function" && o.$$typeof === void 0 ? (t.tag = 1,
        t.memoizedState = null,
        t.updateQueue = null,
        Be(r) ? (i = !0,
        ps(t)) : i = !1,
        t.memoizedState = o.state !== null && o.state !== void 0 ? o.state : null,
        Lu(t),
        o.updater = Ws,
        t.stateNode = o,
        o._reactInternals = t,
        Ta(t, r, e, n),
        t = ja(null, t, r, !0, i, n)) : (t.tag = 0,
        se && i && Nu(t),
        Le(null, t, o, n),
        t = t.child),
        t;
    case 16:
        r = t.elementType;
        e: {
            switch (Xi(e, t),
            e = t.pendingProps,
            o = r._init,
            r = o(r._payload),
            t.type = r,
            o = t.tag = P0(r),
            e = dt(r, e),
            o) {
            case 0:
                t = Aa(null, t, r, e, n);
                break e;
            case 1:
                t = Nd(null, t, r, e, n);
                break e;
            case 11:
                t = kd(null, t, r, e, n);
                break e;
            case 14:
                t = Pd(null, t, r, dt(r.type, e), n);
                break e
            }
            throw Error(A(306, r, ""))
        }
        return t;
    case 0:
        return r = t.type,
        o = t.pendingProps,
        o = t.elementType === r ? o : dt(r, o),
        Aa(e, t, r, o, n);
    case 1:
        return r = t.type,
        o = t.pendingProps,
        o = t.elementType === r ? o : dt(r, o),
        Nd(e, t, r, o, n);
    case 3:
        e: {
            if (Ch(t),
            e === null)
                throw Error(A(387));
            r = t.pendingProps,
            i = t.memoizedState,
            o = i.element,
            Xp(e, t),
            ys(t, r, null, n);
            var s = t.memoizedState;
            if (r = s.element,
            i.isDehydrated)
                if (i = {
                    element: r,
                    isDehydrated: !1,
                    cache: s.cache,
                    pendingSuspenseBoundaries: s.pendingSuspenseBoundaries,
                    transitions: s.transitions
                },
                t.updateQueue.baseState = i,
                t.memoizedState = i,
                t.flags & 256) {
                    o = Qr(Error(A(423)), t),
                    t = Td(e, t, r, n, o);
                    break e
                } else if (r !== o) {
                    o = Qr(Error(A(424)), t),
                    t = Td(e, t, r, n, o);
                    break e
                } else
                    for (Ye = Sn(t.stateNode.containerInfo.firstChild),
                    Xe = t,
                    se = !0,
                    mt = null,
                    n = Gp(t, null, r, n),
                    t.child = n; n; )
                        n.flags = n.flags & -3 | 4096,
                        n = n.sibling;
            else {
                if (Wr(),
                r === o) {
                    t = Qt(e, t, n);
                    break e
                }
                Le(e, t, r, n)
            }
            t = t.child
        }
        return t;
    case 5:
        return qp(t),
        e === null && ka(t),
        r = t.type,
        o = t.pendingProps,
        i = e !== null ? e.memoizedProps : null,
        s = o.children,
        wa(r, o) ? s = null : i !== null && wa(r, i) && (t.flags |= 32),
        Sh(e, t),
        Le(e, t, s, n),
        t.child;
    case 6:
        return e === null && ka(t),
        null;
    case 13:
        return bh(e, t, n);
    case 4:
        return Mu(t, t.stateNode.containerInfo),
        r = t.pendingProps,
        e === null ? t.child = Vr(t, null, r, n) : Le(e, t, r, n),
        t.child;
    case 11:
        return r = t.type,
        o = t.pendingProps,
        o = t.elementType === r ? o : dt(r, o),
        kd(e, t, r, o, n);
    case 7:
        return Le(e, t, t.pendingProps, n),
        t.child;
    case 8:
        return Le(e, t, t.pendingProps.children, n),
        t.child;
    case 12:
        return Le(e, t, t.pendingProps.children, n),
        t.child;
    case 10:
        e: {
            if (r = t.type._context,
            o = t.pendingProps,
            i = t.memoizedProps,
            s = o.value,
            te(vs, r._currentValue),
            r._currentValue = s,
            i !== null)
                if (yt(i.value, s)) {
                    if (i.children === o.children && !Ue.current) {
                        t = Qt(e, t, n);
                        break e
                    }
                } else
                    for (i = t.child,
                    i !== null && (i.return = t); i !== null; ) {
                        var l = i.dependencies;
                        if (l !== null) {
                            s = i.child;
                            for (var a = l.firstContext; a !== null; ) {
                                if (a.context === r) {
                                    if (i.tag === 1) {
                                        a = Bt(-1, n & -n),
                                        a.tag = 2;
                                        var u = i.updateQueue;
                                        if (u !== null) {
                                            u = u.shared;
                                            var d = u.pending;
                                            d === null ? a.next = a : (a.next = d.next,
                                            d.next = a),
                                            u.pending = a
                                        }
                                    }
                                    i.lanes |= n,
                                    a = i.alternate,
                                    a !== null && (a.lanes |= n),
                                    Pa(i.return, n, t),
                                    l.lanes |= n;
                                    break
                                }
                                a = a.next
                            }
                        } else if (i.tag === 10)
                            s = i.type === t.type ? null : i.child;
                        else if (i.tag === 18) {
                            if (s = i.return,
                            s === null)
                                throw Error(A(341));
                            s.lanes |= n,
                            l = s.alternate,
                            l !== null && (l.lanes |= n),
                            Pa(s, n, t),
                            s = i.sibling
                        } else
                            s = i.child;
                        if (s !== null)
                            s.return = i;
                        else
                            for (s = i; s !== null; ) {
                                if (s === t) {
                                    s = null;
                                    break
                                }
                                if (i = s.sibling,
                                i !== null) {
                                    i.return = s.return,
                                    s = i;
                                    break
                                }
                                s = s.return
                            }
                        i = s
                    }
            Le(e, t, o.children, n),
            t = t.child
        }
        return t;
    case 9:
        return o = t.type,
        r = t.pendingProps.children,
        Tr(t, n),
        o = st(o),
        r = r(o),
        t.flags |= 1,
        Le(e, t, r, n),
        t.child;
    case 14:
        return r = t.type,
        o = dt(r, t.pendingProps),
        o = dt(r.type, o),
        Pd(e, t, r, o, n);
    case 15:
        return wh(e, t, t.type, t.pendingProps, n);
    case 17:
        return r = t.type,
        o = t.pendingProps,
        o = t.elementType === r ? o : dt(r, o),
        Xi(e, t),
        t.tag = 1,
        Be(r) ? (e = !0,
        ps(t)) : e = !1,
        Tr(t, n),
        gh(t, r, o),
        Ta(t, r, o, n),
        ja(null, t, r, !0, e, n);
    case 19:
        return kh(e, t, n);
    case 22:
        return Eh(e, t, n)
    }
    throw Error(A(156, t.tag))
}
;
function Uh(e, t) {
    return hp(e, t)
}
function k0(e, t, n, r) {
    this.tag = e,
    this.key = n,
    this.sibling = this.child = this.return = this.stateNode = this.type = this.elementType = null,
    this.index = 0,
    this.ref = null,
    this.pendingProps = t,
    this.dependencies = this.memoizedState = this.updateQueue = this.memoizedProps = null,
    this.mode = r,
    this.subtreeFlags = this.flags = 0,
    this.deletions = null,
    this.childLanes = this.lanes = 0,
    this.alternate = null
}
function ot(e, t, n, r) {
    return new k0(e,t,n,r)
}
function Xu(e) {
    return e = e.prototype,
    !(!e || !e.isReactComponent)
}
function P0(e) {
    if (typeof e == "function")
        return Xu(e) ? 1 : 0;
    if (e != null) {
        if (e = e.$$typeof,
        e === mu)
            return 11;
        if (e === vu)
            return 14
    }
    return 2
}
function Pn(e, t) {
    var n = e.alternate;
    return n === null ? (n = ot(e.tag, t, e.key, e.mode),
    n.elementType = e.elementType,
    n.type = e.type,
    n.stateNode = e.stateNode,
    n.alternate = e,
    e.alternate = n) : (n.pendingProps = t,
    n.type = e.type,
    n.flags = 0,
    n.subtreeFlags = 0,
    n.deletions = null),
    n.flags = e.flags & 14680064,
    n.childLanes = e.childLanes,
    n.lanes = e.lanes,
    n.child = e.child,
    n.memoizedProps = e.memoizedProps,
    n.memoizedState = e.memoizedState,
    n.updateQueue = e.updateQueue,
    t = e.dependencies,
    n.dependencies = t === null ? null : {
        lanes: t.lanes,
        firstContext: t.firstContext
    },
    n.sibling = e.sibling,
    n.index = e.index,
    n.ref = e.ref,
    n
}
function Ji(e, t, n, r, o, i) {
    var s = 2;
    if (r = e,
    typeof e == "function")
        Xu(e) && (s = 1);
    else if (typeof e == "string")
        s = 5;
    else
        e: switch (e) {
        case pr:
            return Xn(n.children, o, i, t);
        case hu:
            s = 8,
            o |= 8;
            break;
        case Zl:
            return e = ot(12, n, t, o | 2),
            e.elementType = Zl,
            e.lanes = i,
            e;
        case Jl:
            return e = ot(13, n, t, o),
            e.elementType = Jl,
            e.lanes = i,
            e;
        case ea:
            return e = ot(19, n, t, o),
            e.elementType = ea,
            e.lanes = i,
            e;
        case qf:
            return Qs(n, o, i, t);
        default:
            if (typeof e == "object" && e !== null)
                switch (e.$$typeof) {
                case Yf:
                    s = 10;
                    break e;
                case Xf:
                    s = 9;
                    break e;
                case mu:
                    s = 11;
                    break e;
                case vu:
                    s = 14;
                    break e;
                case ln:
                    s = 16,
                    r = null;
                    break e
                }
            throw Error(A(130, e == null ? e : typeof e, ""))
        }
    return t = ot(s, n, t, o),
    t.elementType = e,
    t.type = r,
    t.lanes = i,
    t
}
function Xn(e, t, n, r) {
    return e = ot(7, e, r, t),
    e.lanes = n,
    e
}
function Qs(e, t, n, r) {
    return e = ot(22, e, r, t),
    e.elementType = qf,
    e.lanes = n,
    e.stateNode = {
        isHidden: !1
    },
    e
}
function Ul(e, t, n) {
    return e = ot(6, e, null, t),
    e.lanes = n,
    e
}
function Bl(e, t, n) {
    return t = ot(4, e.children !== null ? e.children : [], e.key, t),
    t.lanes = n,
    t.stateNode = {
        containerInfo: e.containerInfo,
        pendingChildren: null,
        implementation: e.implementation
    },
    t
}
function N0(e, t, n, r, o) {
    this.tag = t,
    this.containerInfo = e,
    this.finishedWork = this.pingCache = this.current = this.pendingChildren = null,
    this.timeoutHandle = -1,
    this.callbackNode = this.pendingContext = this.context = null,
    this.callbackPriority = 0,
    this.eventTimes = Sl(0),
    this.expirationTimes = Sl(-1),
    this.entangledLanes = this.finishedLanes = this.mutableReadLanes = this.expiredLanes = this.pingedLanes = this.suspendedLanes = this.pendingLanes = 0,
    this.entanglements = Sl(0),
    this.identifierPrefix = r,
    this.onRecoverableError = o,
    this.mutableSourceEagerHydrationData = null
}
function qu(e, t, n, r, o, i, s, l, a) {
    return e = new N0(e,t,n,l,a),
    t === 1 ? (t = 1,
    i === !0 && (t |= 8)) : t = 0,
    i = ot(3, null, null, t),
    e.current = i,
    i.stateNode = e,
    i.memoizedState = {
        element: r,
        isDehydrated: n,
        cache: null,
        transitions: null,
        pendingSuspenseBoundaries: null
    },
    Lu(i),
    e
}
function T0(e, t, n) {
    var r = 3 < arguments.length && arguments[3] !== void 0 ? arguments[3] : null;
    return {
        $$typeof: fr,
        key: r == null ? null : "" + r,
        children: e,
        containerInfo: t,
        implementation: n
    }
}
function Bh(e) {
    if (!e)
        return Rn;
    e = e._reactInternals;
    e: {
        if (or(e) !== e || e.tag !== 1)
            throw Error(A(170));
        var t = e;
        do {
            switch (t.tag) {
            case 3:
                t = t.stateNode.context;
                break e;
            case 1:
                if (Be(t.type)) {
                    t = t.stateNode.__reactInternalMemoizedMergedChildContext;
                    break e
                }
            }
            t = t.return
        } while (t !== null);
        throw Error(A(171))
    }
    if (e.tag === 1) {
        var n = e.type;
        if (Be(n))
            return Bp(e, n, t)
    }
    return t
}
function Wh(e, t, n, r, o, i, s, l, a) {
    return e = qu(n, r, !0, e, o, i, s, l, a),
    e.context = Bh(null),
    n = e.current,
    r = Ie(),
    o = kn(n),
    i = Bt(r, o),
    i.callback = t ?? null,
    Cn(n, i, o),
    e.current.lanes = o,
    si(e, o, r),
    We(e, r),
    e
}
function Ks(e, t, n, r) {
    var o = t.current
      , i = Ie()
      , s = kn(o);
    return n = Bh(n),
    t.context === null ? t.context = n : t.pendingContext = n,
    t = Bt(i, s),
    t.payload = {
        element: e
    },
    r = r === void 0 ? null : r,
    r !== null && (t.callback = r),
    e = Cn(o, t, s),
    e !== null && (gt(e, o, s, i),
    Ki(e, o, s)),
    s
}
function Ps(e) {
    if (e = e.current,
    !e.child)
        return null;
    switch (e.child.tag) {
    case 5:
        return e.child.stateNode;
    default:
        return e.child.stateNode
    }
}
function zd(e, t) {
    if (e = e.memoizedState,
    e !== null && e.dehydrated !== null) {
        var n = e.retryLane;
        e.retryLane = n !== 0 && n < t ? n : t
    }
}
function Zu(e, t) {
    zd(e, t),
    (e = e.alternate) && zd(e, t)
}
function R0() {
    return null
}
var Vh = typeof reportError == "function" ? reportError : function(e) {
    console.error(e)
}
;
function Ju(e) {
    this._internalRoot = e
}
Gs.prototype.render = Ju.prototype.render = function(e) {
    var t = this._internalRoot;
    if (t === null)
        throw Error(A(409));
    Ks(e, t, null, null)
}
;
Gs.prototype.unmount = Ju.prototype.unmount = function() {
    var e = this._internalRoot;
    if (e !== null) {
        this._internalRoot = null;
        var t = e.containerInfo;
        tr(function() {
            Ks(null, e, null, null)
        }),
        t[Vt] = null
    }
}
;
function Gs(e) {
    this._internalRoot = e
}
Gs.prototype.unstable_scheduleHydration = function(e) {
    if (e) {
        var t = Ep();
        e = {
            blockedOn: null,
            target: e,
            priority: t
        };
        for (var n = 0; n < un.length && t !== 0 && t < un[n].priority; n++)
            ;
        un.splice(n, 0, e),
        n === 0 && Cp(e)
    }
}
;
function ec(e) {
    return !(!e || e.nodeType !== 1 && e.nodeType !== 9 && e.nodeType !== 11)
}
function Ys(e) {
    return !(!e || e.nodeType !== 1 && e.nodeType !== 9 && e.nodeType !== 11 && (e.nodeType !== 8 || e.nodeValue !== " react-mount-point-unstable "))
}
function Fd() {}
function A0(e, t, n, r, o) {
    if (o) {
        if (typeof r == "function") {
            var i = r;
            r = function() {
                var u = Ps(s);
                i.call(u)
            }
        }
        var s = Wh(t, r, e, 0, null, !1, !1, "", Fd);
        return e._reactRootContainer = s,
        e[Vt] = s.current,
        Uo(e.nodeType === 8 ? e.parentNode : e),
        tr(),
        s
    }
    for (; o = e.lastChild; )
        e.removeChild(o);
    if (typeof r == "function") {
        var l = r;
        r = function() {
            var u = Ps(a);
            l.call(u)
        }
    }
    var a = qu(e, 0, !1, null, null, !1, !1, "", Fd);
    return e._reactRootContainer = a,
    e[Vt] = a.current,
    Uo(e.nodeType === 8 ? e.parentNode : e),
    tr(function() {
        Ks(t, a, n, r)
    }),
    a
}
function Xs(e, t, n, r, o) {
    var i = n._reactRootContainer;
    if (i) {
        var s = i;
        if (typeof o == "function") {
            var l = o;
            o = function() {
                var a = Ps(s);
                l.call(a)
            }
        }
        Ks(t, s, e, o)
    } else
        s = A0(n, t, e, o, r);
    return Ps(s)
}
xp = function(e) {
    switch (e.tag) {
    case 3:
        var t = e.stateNode;
        if (t.current.memoizedState.isDehydrated) {
            var n = Eo(t.pendingLanes);
            n !== 0 && (xu(t, n | 1),
            We(t, he()),
            !(X & 6) && (Kr = he() + 500,
            In()))
        }
        break;
    case 13:
        tr(function() {
            var r = Ht(e, 1);
            if (r !== null) {
                var o = Ie();
                gt(r, e, 1, o)
            }
        }),
        Zu(e, 1)
    }
}
;
wu = function(e) {
    if (e.tag === 13) {
        var t = Ht(e, 134217728);
        if (t !== null) {
            var n = Ie();
            gt(t, e, 134217728, n)
        }
        Zu(e, 134217728)
    }
}
;
wp = function(e) {
    if (e.tag === 13) {
        var t = kn(e)
          , n = Ht(e, t);
        if (n !== null) {
            var r = Ie();
            gt(n, e, t, r)
        }
        Zu(e, t)
    }
}
;
Ep = function() {
    return J
}
;
Sp = function(e, t) {
    var n = J;
    try {
        return J = e,
        t()
    } finally {
        J = n
    }
}
;
ca = function(e, t, n) {
    switch (t) {
    case "input":
        if (ra(e, n),
        t = n.name,
        n.type === "radio" && t != null) {
            for (n = e; n.parentNode; )
                n = n.parentNode;
            for (n = n.querySelectorAll("input[name=" + JSON.stringify("" + t) + '][type="radio"]'),
            t = 0; t < n.length; t++) {
                var r = n[t];
                if (r !== e && r.form === e.form) {
                    var o = $s(r);
                    if (!o)
                        throw Error(A(90));
                    Jf(r),
                    ra(r, o)
                }
            }
        }
        break;
    case "textarea":
        tp(e, n);
        break;
    case "select":
        t = n.value,
        t != null && br(e, !!n.multiple, t, !1)
    }
}
;
ap = Ku;
up = tr;
var j0 = {
    usingClientEntryPoint: !1,
    Events: [ai, gr, $s, sp, lp, Ku]
}
  , mo = {
    findFiberByHostInstance: Bn,
    bundleType: 0,
    version: "18.3.1",
    rendererPackageName: "react-dom"
}
  , O0 = {
    bundleType: mo.bundleType,
    version: mo.version,
    rendererPackageName: mo.rendererPackageName,
    rendererConfig: mo.rendererConfig,
    overrideHookState: null,
    overrideHookStateDeletePath: null,
    overrideHookStateRenamePath: null,
    overrideProps: null,
    overridePropsDeletePath: null,
    overridePropsRenamePath: null,
    setErrorHandler: null,
    setSuspenseHandler: null,
    scheduleUpdate: null,
    currentDispatcherRef: Yt.ReactCurrentDispatcher,
    findHostInstanceByFiber: function(e) {
        return e = fp(e),
        e === null ? null : e.stateNode
    },
    findFiberByHostInstance: mo.findFiberByHostInstance || R0,
    findHostInstancesForRefresh: null,
    scheduleRefresh: null,
    scheduleRoot: null,
    setRefreshHandler: null,
    getCurrentFiber: null,
    reconcilerVersion: "18.3.1-next-f1338f8080-20240426"
};
if (typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ < "u") {
    var Mi = __REACT_DEVTOOLS_GLOBAL_HOOK__;
    if (!Mi.isDisabled && Mi.supportsFiber)
        try {
            Is = Mi.inject(O0),
            Ot = Mi
        } catch {}
}
Je.__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED = j0;
Je.createPortal = function(e, t) {
    var n = 2 < arguments.length && arguments[2] !== void 0 ? arguments[2] : null;
    if (!ec(t))
        throw Error(A(200));
    return T0(e, t, null, n)
}
;
Je.createRoot = function(e, t) {
    if (!ec(e))
        throw Error(A(299));
    var n = !1
      , r = ""
      , o = Vh;
    return t != null && (t.unstable_strictMode === !0 && (n = !0),
    t.identifierPrefix !== void 0 && (r = t.identifierPrefix),
    t.onRecoverableError !== void 0 && (o = t.onRecoverableError)),
    t = qu(e, 1, !1, null, null, n, !1, r, o),
    e[Vt] = t.current,
    Uo(e.nodeType === 8 ? e.parentNode : e),
    new Ju(t)
}
;
Je.findDOMNode = function(e) {
    if (e == null)
        return null;
    if (e.nodeType === 1)
        return e;
    var t = e._reactInternals;
    if (t === void 0)
        throw typeof e.render == "function" ? Error(A(188)) : (e = Object.keys(e).join(","),
        Error(A(268, e)));
    return e = fp(t),
    e = e === null ? null : e.stateNode,
    e
}
;
Je.flushSync = function(e) {
    return tr(e)
}
;
Je.hydrate = function(e, t, n) {
    if (!Ys(t))
        throw Error(A(200));
    return Xs(null, e, t, !0, n)
}
;
Je.hydrateRoot = function(e, t, n) {
    if (!ec(e))
        throw Error(A(405));
    var r = n != null && n.hydratedSources || null
      , o = !1
      , i = ""
      , s = Vh;
    if (n != null && (n.unstable_strictMode === !0 && (o = !0),
    n.identifierPrefix !== void 0 && (i = n.identifierPrefix),
    n.onRecoverableError !== void 0 && (s = n.onRecoverableError)),
    t = Wh(t, null, e, 1, n ?? null, o, !1, i, s),
    e[Vt] = t.current,
    Uo(e),
    r)
        for (e = 0; e < r.length; e++)
            n = r[e],
            o = n._getVersion,
            o = o(n._source),
            t.mutableSourceEagerHydrationData == null ? t.mutableSourceEagerHydrationData = [n, o] : t.mutableSourceEagerHydrationData.push(n, o);
    return new Gs(t)
}
;
Je.render = function(e, t, n) {
    if (!Ys(t))
        throw Error(A(200));
    return Xs(null, e, t, !1, n)
}
;
Je.unmountComponentAtNode = function(e) {
    if (!Ys(e))
        throw Error(A(40));
    return e._reactRootContainer ? (tr(function() {
        Xs(null, null, e, !1, function() {
            e._reactRootContainer = null,
            e[Vt] = null
        })
    }),
    !0) : !1
}
;
Je.unstable_batchedUpdates = Ku;
Je.unstable_renderSubtreeIntoContainer = function(e, t, n, r) {
    if (!Ys(n))
        throw Error(A(200));
    if (e == null || e._reactInternals === void 0)
        throw Error(A(38));
    return Xs(e, t, n, !1, r)
}
;
Je.version = "18.3.1-next-f1338f8080-20240426";
function Hh() {
    if (!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__ > "u" || typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE != "function"))
        try {
            __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(Hh)
        } catch (e) {
            console.error(e)
        }
}
Hh(),
Hf.exports = Je;
var Jr = Hf.exports;
const Qh = Of(Jr);
var Kh, $d = Jr;
Kh = $d.createRoot,
$d.hydrateRoot;
const _0 = 1
  , L0 = 1e6;
let Wl = 0;
function M0() {
    return Wl = (Wl + 1) % Number.MAX_SAFE_INTEGER,
    Wl.toString()
}
const Vl = new Map
  , Ud = e => {
    if (Vl.has(e))
        return;
    const t = setTimeout( () => {
        Vl.delete(e),
        Oo({
            type: "REMOVE_TOAST",
            toastId: e
        })
    }
    , L0);
    Vl.set(e, t)
}
  , I0 = (e, t) => {
    switch (t.type) {
    case "ADD_TOAST":
        return {
            ...e,
            toasts: [t.toast, ...e.toasts].slice(0, _0)
        };
    case "UPDATE_TOAST":
        return {
            ...e,
            toasts: e.toasts.map(n => n.id === t.toast.id ? {
                ...n,
                ...t.toast
            } : n)
        };
    case "DISMISS_TOAST":
        {
            const {toastId: n} = t;
            return n ? Ud(n) : e.toasts.forEach(r => {
                Ud(r.id)
            }
            ),
            {
                ...e,
                toasts: e.toasts.map(r => r.id === n || n === void 0 ? {
                    ...r,
                    open: !1
                } : r)
            }
        }
    case "REMOVE_TOAST":
        return t.toastId === void 0 ? {
            ...e,
            toasts: []
        } : {
            ...e,
            toasts: e.toasts.filter(n => n.id !== t.toastId)
        }
    }
}
  , es = [];
let ts = {
    toasts: []
};
function Oo(e) {
    ts = I0(ts, e),
    es.forEach(t => {
        t(ts)
    }
    )
}
function D0({...e}) {
    const t = M0()
      , n = o => Oo({
        type: "UPDATE_TOAST",
        toast: {
            ...o,
            id: t
        }
    })
      , r = () => Oo({
        type: "DISMISS_TOAST",
        toastId: t
    });
    return Oo({
        type: "ADD_TOAST",
        toast: {
            ...e,
            id: t,
            open: !0,
            onOpenChange: o => {
                o || r()
            }
        }
    }),
    {
        id: t,
        dismiss: r,
        update: n
    }
}
function z0() {
    const [e,t] = g.useState(ts);
    return g.useEffect( () => (es.push(t),
    () => {
        const n = es.indexOf(t);
        n > -1 && es.splice(n, 1)
    }
    ), [e]),
    {
        ...e,
        toast: D0,
        dismiss: n => Oo({
            type: "DISMISS_TOAST",
            toastId: n
        })
    }
}
function le(e, t, {checkForDefaultPrevented: n=!0}={}) {
    return function(o) {
        if (e == null || e(o),
        n === !1 || !o.defaultPrevented)
            return t == null ? void 0 : t(o)
    }
}
function F0(e, t) {
    typeof e == "function" ? e(t) : e != null && (e.current = t)
}
function Gh(...e) {
    return t => e.forEach(n => F0(n, t))
}
function Oe(...e) {
    return g.useCallback(Gh(...e), e)
}
function ci(e, t=[]) {
    let n = [];
    function r(i, s) {
        const l = g.createContext(s)
          , a = n.length;
        n = [...n, s];
        function u(f) {
            const {scope: c, children: y, ...w} = f
              , x = (c == null ? void 0 : c[e][a]) || l
              , E = g.useMemo( () => w, Object.values(w));
            return m.jsx(x.Provider, {
                value: E,
                children: y
            })
        }
        function d(f, c) {
            const y = (c == null ? void 0 : c[e][a]) || l
              , w = g.useContext(y);
            if (w)
                return w;
            if (s !== void 0)
                return s;
            throw new Error(`\`${f}\` must be used within \`${i}\``)
        }
        return u.displayName = i + "Provider",
        [u, d]
    }
    const o = () => {
        const i = n.map(s => g.createContext(s));
        return function(l) {
            const a = (l == null ? void 0 : l[e]) || i;
            return g.useMemo( () => ({
                [`__scope${e}`]: {
                    ...l,
                    [e]: a
                }
            }), [l, a])
        }
    }
    ;
    return o.scopeName = e,
    [r, $0(o, ...t)]
}
function $0(...e) {
    const t = e[0];
    if (e.length === 1)
        return t;
    const n = () => {
        const r = e.map(o => ({
            useScope: o(),
            scopeName: o.scopeName
        }));
        return function(i) {
            const s = r.reduce( (l, {useScope: a, scopeName: u}) => {
                const f = a(i)[`__scope${u}`];
                return {
                    ...l,
                    ...f
                }
            }
            , {});
            return g.useMemo( () => ({
                [`__scope${t.scopeName}`]: s
            }), [s])
        }
    }
    ;
    return n.scopeName = t.scopeName,
    n
}
var Xo = g.forwardRef( (e, t) => {
    const {children: n, ...r} = e
      , o = g.Children.toArray(n)
      , i = o.find(U0);
    if (i) {
        const s = i.props.children
          , l = o.map(a => a === i ? g.Children.count(s) > 1 ? g.Children.only(null) : g.isValidElement(s) ? s.props.children : null : a);
        return m.jsx(Wa, {
            ...r,
            ref: t,
            children: g.isValidElement(s) ? g.cloneElement(s, void 0, l) : null
        })
    }
    return m.jsx(Wa, {
        ...r,
        ref: t,
        children: n
    })
}
);
Xo.displayName = "Slot";
var Wa = g.forwardRef( (e, t) => {
    const {children: n, ...r} = e;
    if (g.isValidElement(n)) {
        const o = W0(n);
        return g.cloneElement(n, {
            ...B0(r, n.props),
            ref: t ? Gh(t, o) : o
        })
    }
    return g.Children.count(n) > 1 ? g.Children.only(null) : null
}
);
Wa.displayName = "SlotClone";
var Yh = ({children: e}) => m.jsx(m.Fragment, {
    children: e
});
function U0(e) {
    return g.isValidElement(e) && e.type === Yh
}
function B0(e, t) {
    const n = {
        ...t
    };
    for (const r in t) {
        const o = e[r]
          , i = t[r];
        /^on[A-Z]/.test(r) ? o && i ? n[r] = (...l) => {
            i(...l),
            o(...l)
        }
        : o && (n[r] = o) : r === "style" ? n[r] = {
            ...o,
            ...i
        } : r === "className" && (n[r] = [o, i].filter(Boolean).join(" "))
    }
    return {
        ...e,
        ...n
    }
}
function W0(e) {
    var r, o;
    let t = (r = Object.getOwnPropertyDescriptor(e.props, "ref")) == null ? void 0 : r.get
      , n = t && "isReactWarning"in t && t.isReactWarning;
    return n ? e.ref : (t = (o = Object.getOwnPropertyDescriptor(e, "ref")) == null ? void 0 : o.get,
    n = t && "isReactWarning"in t && t.isReactWarning,
    n ? e.props.ref : e.props.ref || e.ref)
}
function Xh(e) {
    const t = e + "CollectionProvider"
      , [n,r] = ci(t)
      , [o,i] = n(t, {
        collectionRef: {
            current: null
        },
        itemMap: new Map
    })
      , s = y => {
        const {scope: w, children: x} = y
          , E = R.useRef(null)
          , h = R.useRef(new Map).current;
        return m.jsx(o, {
            scope: w,
            itemMap: h,
            collectionRef: E,
            children: x
        })
    }
    ;
    s.displayName = t;
    const l = e + "CollectionSlot"
      , a = R.forwardRef( (y, w) => {
        const {scope: x, children: E} = y
          , h = i(l, x)
          , p = Oe(w, h.collectionRef);
        return m.jsx(Xo, {
            ref: p,
            children: E
        })
    }
    );
    a.displayName = l;
    const u = e + "CollectionItemSlot"
      , d = "data-radix-collection-item"
      , f = R.forwardRef( (y, w) => {
        const {scope: x, children: E, ...h} = y
          , p = R.useRef(null)
          , v = Oe(w, p)
          , S = i(u, x);
        return R.useEffect( () => (S.itemMap.set(p, {
            ref: p,
            ...h
        }),
        () => void S.itemMap.delete(p))),
        m.jsx(Xo, {
            [d]: "",
            ref: v,
            children: E
        })
    }
    );
    f.displayName = u;
    function c(y) {
        const w = i(e + "CollectionConsumer", y);
        return R.useCallback( () => {
            const E = w.collectionRef.current;
            if (!E)
                return [];
            const h = Array.from(E.querySelectorAll(`[${d}]`));
            return Array.from(w.itemMap.values()).sort( (S, C) => h.indexOf(S.ref.current) - h.indexOf(C.ref.current))
        }
        , [w.collectionRef, w.itemMap])
    }
    return [{
        Provider: s,
        Slot: a,
        ItemSlot: f
    }, c, r]
}
var V0 = ["a", "button", "div", "form", "h2", "h3", "img", "input", "label", "li", "nav", "ol", "p", "span", "svg", "ul"]
  , me = V0.reduce( (e, t) => {
    const n = g.forwardRef( (r, o) => {
        const {asChild: i, ...s} = r
          , l = i ? Xo : t;
        return typeof window < "u" && (window[Symbol.for("radix-ui")] = !0),
        m.jsx(l, {
            ...s,
            ref: o
        })
    }
    );
    return n.displayName = `Primitive.${t}`,
    {
        ...e,
        [t]: n
    }
}
, {});
function tc(e, t) {
    e && Jr.flushSync( () => e.dispatchEvent(t))
}
function at(e) {
    const t = g.useRef(e);
    return g.useEffect( () => {
        t.current = e
    }
    ),
    g.useMemo( () => (...n) => {
        var r;
        return (r = t.current) == null ? void 0 : r.call(t, ...n)
    }
    , [])
}
function qh(e, t=globalThis == null ? void 0 : globalThis.document) {
    const n = at(e);
    g.useEffect( () => {
        const r = o => {
            o.key === "Escape" && n(o)
        }
        ;
        return t.addEventListener("keydown", r, {
            capture: !0
        }),
        () => t.removeEventListener("keydown", r, {
            capture: !0
        })
    }
    , [n, t])
}
var H0 = "DismissableLayer", Va = "dismissableLayer.update", Q0 = "dismissableLayer.pointerDownOutside", K0 = "dismissableLayer.focusOutside", Bd, Zh = g.createContext({
    layers: new Set,
    layersWithOutsidePointerEventsDisabled: new Set,
    branches: new Set
}), Jh = g.forwardRef( (e, t) => {
    const {disableOutsidePointerEvents: n=!1, onEscapeKeyDown: r, onPointerDownOutside: o, onFocusOutside: i, onInteractOutside: s, onDismiss: l, ...a} = e
      , u = g.useContext(Zh)
      , [d,f] = g.useState(null)
      , c = (d == null ? void 0 : d.ownerDocument) ?? (globalThis == null ? void 0 : globalThis.document)
      , [,y] = g.useState({})
      , w = Oe(t, b => f(b))
      , x = Array.from(u.layers)
      , [E] = [...u.layersWithOutsidePointerEventsDisabled].slice(-1)
      , h = x.indexOf(E)
      , p = d ? x.indexOf(d) : -1
      , v = u.layersWithOutsidePointerEventsDisabled.size > 0
      , S = p >= h
      , C = Y0(b => {
        const N = b.target
          , _ = [...u.branches].some(O => O.contains(N));
        !S || _ || (o == null || o(b),
        s == null || s(b),
        b.defaultPrevented || l == null || l())
    }
    , c)
      , P = X0(b => {
        const N = b.target;
        [...u.branches].some(O => O.contains(N)) || (i == null || i(b),
        s == null || s(b),
        b.defaultPrevented || l == null || l())
    }
    , c);
    return qh(b => {
        p === u.layers.size - 1 && (r == null || r(b),
        !b.defaultPrevented && l && (b.preventDefault(),
        l()))
    }
    , c),
    g.useEffect( () => {
        if (d)
            return n && (u.layersWithOutsidePointerEventsDisabled.size === 0 && (Bd = c.body.style.pointerEvents,
            c.body.style.pointerEvents = "none"),
            u.layersWithOutsidePointerEventsDisabled.add(d)),
            u.layers.add(d),
            Wd(),
            () => {
                n && u.layersWithOutsidePointerEventsDisabled.size === 1 && (c.body.style.pointerEvents = Bd)
            }
    }
    , [d, c, n, u]),
    g.useEffect( () => () => {
        d && (u.layers.delete(d),
        u.layersWithOutsidePointerEventsDisabled.delete(d),
        Wd())
    }
    , [d, u]),
    g.useEffect( () => {
        const b = () => y({});
        return document.addEventListener(Va, b),
        () => document.removeEventListener(Va, b)
    }
    , []),
    m.jsx(me.div, {
        ...a,
        ref: w,
        style: {
            pointerEvents: v ? S ? "auto" : "none" : void 0,
            ...e.style
        },
        onFocusCapture: le(e.onFocusCapture, P.onFocusCapture),
        onBlurCapture: le(e.onBlurCapture, P.onBlurCapture),
        onPointerDownCapture: le(e.onPointerDownCapture, C.onPointerDownCapture)
    })
}
);
Jh.displayName = H0;
var G0 = "DismissableLayerBranch"
  , em = g.forwardRef( (e, t) => {
    const n = g.useContext(Zh)
      , r = g.useRef(null)
      , o = Oe(t, r);
    return g.useEffect( () => {
        const i = r.current;
        if (i)
            return n.branches.add(i),
            () => {
                n.branches.delete(i)
            }
    }
    , [n.branches]),
    m.jsx(me.div, {
        ...e,
        ref: o
    })
}
);
em.displayName = G0;
function Y0(e, t=globalThis == null ? void 0 : globalThis.document) {
    const n = at(e)
      , r = g.useRef(!1)
      , o = g.useRef( () => {}
    );
    return g.useEffect( () => {
        const i = l => {
            if (l.target && !r.current) {
                let a = function() {
                    tm(Q0, n, u, {
                        discrete: !0
                    })
                };
                const u = {
                    originalEvent: l
                };
                l.pointerType === "touch" ? (t.removeEventListener("click", o.current),
                o.current = a,
                t.addEventListener("click", o.current, {
                    once: !0
                })) : a()
            } else
                t.removeEventListener("click", o.current);
            r.current = !1
        }
          , s = window.setTimeout( () => {
            t.addEventListener("pointerdown", i)
        }
        , 0);
        return () => {
            window.clearTimeout(s),
            t.removeEventListener("pointerdown", i),
            t.removeEventListener("click", o.current)
        }
    }
    , [t, n]),
    {
        onPointerDownCapture: () => r.current = !0
    }
}
function X0(e, t=globalThis == null ? void 0 : globalThis.document) {
    const n = at(e)
      , r = g.useRef(!1);
    return g.useEffect( () => {
        const o = i => {
            i.target && !r.current && tm(K0, n, {
                originalEvent: i
            }, {
                discrete: !1
            })
        }
        ;
        return t.addEventListener("focusin", o),
        () => t.removeEventListener("focusin", o)
    }
    , [t, n]),
    {
        onFocusCapture: () => r.current = !0,
        onBlurCapture: () => r.current = !1
    }
}
function Wd() {
    const e = new CustomEvent(Va);
    document.dispatchEvent(e)
}
function tm(e, t, n, {discrete: r}) {
    const o = n.originalEvent.target
      , i = new CustomEvent(e,{
        bubbles: !1,
        cancelable: !0,
        detail: n
    });
    t && o.addEventListener(e, t, {
        once: !0
    }),
    r ? tc(o, i) : o.dispatchEvent(i)
}
var q0 = Jh
  , Z0 = em
  , xt = globalThis != null && globalThis.document ? g.useLayoutEffect : () => {}
  , J0 = "Portal"
  , nm = g.forwardRef( (e, t) => {
    var l;
    const {container: n, ...r} = e
      , [o,i] = g.useState(!1);
    xt( () => i(!0), []);
    const s = n || o && ((l = globalThis == null ? void 0 : globalThis.document) == null ? void 0 : l.body);
    return s ? Qh.createPortal(m.jsx(me.div, {
        ...r,
        ref: t
    }), s) : null
}
);
nm.displayName = J0;
function ex(e, t) {
    return g.useReducer( (n, r) => t[n][r] ?? n, e)
}
var nc = e => {
    const {present: t, children: n} = e
      , r = tx(t)
      , o = typeof n == "function" ? n({
        present: r.isPresent
    }) : g.Children.only(n)
      , i = Oe(r.ref, nx(o));
    return typeof n == "function" || r.isPresent ? g.cloneElement(o, {
        ref: i
    }) : null
}
;
nc.displayName = "Presence";
function tx(e) {
    const [t,n] = g.useState()
      , r = g.useRef({})
      , o = g.useRef(e)
      , i = g.useRef("none")
      , s = e ? "mounted" : "unmounted"
      , [l,a] = ex(s, {
        mounted: {
            UNMOUNT: "unmounted",
            ANIMATION_OUT: "unmountSuspended"
        },
        unmountSuspended: {
            MOUNT: "mounted",
            ANIMATION_END: "unmounted"
        },
        unmounted: {
            MOUNT: "mounted"
        }
    });
    return g.useEffect( () => {
        const u = Ii(r.current);
        i.current = l === "mounted" ? u : "none"
    }
    , [l]),
    xt( () => {
        const u = r.current
          , d = o.current;
        if (d !== e) {
            const c = i.current
              , y = Ii(u);
            e ? a("MOUNT") : y === "none" || (u == null ? void 0 : u.display) === "none" ? a("UNMOUNT") : a(d && c !== y ? "ANIMATION_OUT" : "UNMOUNT"),
            o.current = e
        }
    }
    , [e, a]),
    xt( () => {
        if (t) {
            const u = f => {
                const y = Ii(r.current).includes(f.animationName);
                f.target === t && y && Jr.flushSync( () => a("ANIMATION_END"))
            }
              , d = f => {
                f.target === t && (i.current = Ii(r.current))
            }
            ;
            return t.addEventListener("animationstart", d),
            t.addEventListener("animationcancel", u),
            t.addEventListener("animationend", u),
            () => {
                t.removeEventListener("animationstart", d),
                t.removeEventListener("animationcancel", u),
                t.removeEventListener("animationend", u)
            }
        } else
            a("ANIMATION_END")
    }
    , [t, a]),
    {
        isPresent: ["mounted", "unmountSuspended"].includes(l),
        ref: g.useCallback(u => {
            u && (r.current = getComputedStyle(u)),
            n(u)
        }
        , [])
    }
}
function Ii(e) {
    return (e == null ? void 0 : e.animationName) || "none"
}
function nx(e) {
    var r, o;
    let t = (r = Object.getOwnPropertyDescriptor(e.props, "ref")) == null ? void 0 : r.get
      , n = t && "isReactWarning"in t && t.isReactWarning;
    return n ? e.ref : (t = (o = Object.getOwnPropertyDescriptor(e, "ref")) == null ? void 0 : o.get,
    n = t && "isReactWarning"in t && t.isReactWarning,
    n ? e.props.ref : e.props.ref || e.ref)
}
function qs({prop: e, defaultProp: t, onChange: n= () => {}
}) {
    const [r,o] = rx({
        defaultProp: t,
        onChange: n
    })
      , i = e !== void 0
      , s = i ? e : r
      , l = at(n)
      , a = g.useCallback(u => {
        if (i) {
            const f = typeof u == "function" ? u(e) : u;
            f !== e && l(f)
        } else
            o(u)
    }
    , [i, e, o, l]);
    return [s, a]
}
function rx({defaultProp: e, onChange: t}) {
    const n = g.useState(e)
      , [r] = n
      , o = g.useRef(r)
      , i = at(t);
    return g.useEffect( () => {
        o.current !== r && (i(r),
        o.current = r)
    }
    , [r, o, i]),
    n
}
var ox = "VisuallyHidden"
  , Zs = g.forwardRef( (e, t) => m.jsx(me.span, {
    ...e,
    ref: t,
    style: {
        position: "absolute",
        border: 0,
        width: 1,
        height: 1,
        padding: 0,
        margin: -1,
        overflow: "hidden",
        clip: "rect(0, 0, 0, 0)",
        whiteSpace: "nowrap",
        wordWrap: "normal",
        ...e.style
    }
}));
Zs.displayName = ox;
var ix = Zs
  , rc = "ToastProvider"
  , [oc,sx,lx] = Xh("Toast")
  , [rm,zC] = ci("Toast", [lx])
  , [ax,Js] = rm(rc)
  , om = e => {
    const {__scopeToast: t, label: n="Notification", duration: r=5e3, swipeDirection: o="right", swipeThreshold: i=50, children: s} = e
      , [l,a] = g.useState(null)
      , [u,d] = g.useState(0)
      , f = g.useRef(!1)
      , c = g.useRef(!1);
    return n.trim() || console.error(`Invalid prop \`label\` supplied to \`${rc}\`. Expected non-empty \`string\`.`),
    m.jsx(oc.Provider, {
        scope: t,
        children: m.jsx(ax, {
            scope: t,
            label: n,
            duration: r,
            swipeDirection: o,
            swipeThreshold: i,
            toastCount: u,
            viewport: l,
            onViewportChange: a,
            onToastAdd: g.useCallback( () => d(y => y + 1), []),
            onToastRemove: g.useCallback( () => d(y => y - 1), []),
            isFocusedToastEscapeKeyDownRef: f,
            isClosePausedRef: c,
            children: s
        })
    })
}
;
om.displayName = rc;
var im = "ToastViewport"
  , ux = ["F8"]
  , Ha = "toast.viewportPause"
  , Qa = "toast.viewportResume"
  , sm = g.forwardRef( (e, t) => {
    const {__scopeToast: n, hotkey: r=ux, label: o="Notifications ({hotkey})", ...i} = e
      , s = Js(im, n)
      , l = sx(n)
      , a = g.useRef(null)
      , u = g.useRef(null)
      , d = g.useRef(null)
      , f = g.useRef(null)
      , c = Oe(t, f, s.onViewportChange)
      , y = r.join("+").replace(/Key/g, "").replace(/Digit/g, "")
      , w = s.toastCount > 0;
    g.useEffect( () => {
        const E = h => {
            var v;
            r.every(S => h[S] || h.code === S) && ((v = f.current) == null || v.focus())
        }
        ;
        return document.addEventListener("keydown", E),
        () => document.removeEventListener("keydown", E)
    }
    , [r]),
    g.useEffect( () => {
        const E = a.current
          , h = f.current;
        if (w && E && h) {
            const p = () => {
                if (!s.isClosePausedRef.current) {
                    const P = new CustomEvent(Ha);
                    h.dispatchEvent(P),
                    s.isClosePausedRef.current = !0
                }
            }
              , v = () => {
                if (s.isClosePausedRef.current) {
                    const P = new CustomEvent(Qa);
                    h.dispatchEvent(P),
                    s.isClosePausedRef.current = !1
                }
            }
              , S = P => {
                !E.contains(P.relatedTarget) && v()
            }
              , C = () => {
                E.contains(document.activeElement) || v()
            }
            ;
            return E.addEventListener("focusin", p),
            E.addEventListener("focusout", S),
            E.addEventListener("pointermove", p),
            E.addEventListener("pointerleave", C),
            window.addEventListener("blur", p),
            window.addEventListener("focus", v),
            () => {
                E.removeEventListener("focusin", p),
                E.removeEventListener("focusout", S),
                E.removeEventListener("pointermove", p),
                E.removeEventListener("pointerleave", C),
                window.removeEventListener("blur", p),
                window.removeEventListener("focus", v)
            }
        }
    }
    , [w, s.isClosePausedRef]);
    const x = g.useCallback( ({tabbingDirection: E}) => {
        const p = l().map(v => {
            const S = v.ref.current
              , C = [S, ...Sx(S)];
            return E === "forwards" ? C : C.reverse()
        }
        );
        return (E === "forwards" ? p.reverse() : p).flat()
    }
    , [l]);
    return g.useEffect( () => {
        const E = f.current;
        if (E) {
            const h = p => {
                var C, P, b;
                const v = p.altKey || p.ctrlKey || p.metaKey;
                if (p.key === "Tab" && !v) {
                    const N = document.activeElement
                      , _ = p.shiftKey;
                    if (p.target === E && _) {
                        (C = u.current) == null || C.focus();
                        return
                    }
                    const D = x({
                        tabbingDirection: _ ? "backwards" : "forwards"
                    })
                      , H = D.findIndex(L => L === N);
                    Hl(D.slice(H + 1)) ? p.preventDefault() : _ ? (P = u.current) == null || P.focus() : (b = d.current) == null || b.focus()
                }
            }
            ;
            return E.addEventListener("keydown", h),
            () => E.removeEventListener("keydown", h)
        }
    }
    , [l, x]),
    m.jsxs(Z0, {
        ref: a,
        role: "region",
        "aria-label": o.replace("{hotkey}", y),
        tabIndex: -1,
        style: {
            pointerEvents: w ? void 0 : "none"
        },
        children: [w && m.jsx(Ka, {
            ref: u,
            onFocusFromOutsideViewport: () => {
                const E = x({
                    tabbingDirection: "forwards"
                });
                Hl(E)
            }
        }), m.jsx(oc.Slot, {
            scope: n,
            children: m.jsx(me.ol, {
                tabIndex: -1,
                ...i,
                ref: c
            })
        }), w && m.jsx(Ka, {
            ref: d,
            onFocusFromOutsideViewport: () => {
                const E = x({
                    tabbingDirection: "backwards"
                });
                Hl(E)
            }
        })]
    })
}
);
sm.displayName = im;
var lm = "ToastFocusProxy"
  , Ka = g.forwardRef( (e, t) => {
    const {__scopeToast: n, onFocusFromOutsideViewport: r, ...o} = e
      , i = Js(lm, n);
    return m.jsx(Zs, {
        "aria-hidden": !0,
        tabIndex: 0,
        ...o,
        ref: t,
        style: {
            position: "fixed"
        },
        onFocus: s => {
            var u;
            const l = s.relatedTarget;
            !((u = i.viewport) != null && u.contains(l)) && r()
        }
    })
}
);
Ka.displayName = lm;
var el = "Toast"
  , cx = "toast.swipeStart"
  , dx = "toast.swipeMove"
  , fx = "toast.swipeCancel"
  , px = "toast.swipeEnd"
  , am = g.forwardRef( (e, t) => {
    const {forceMount: n, open: r, defaultOpen: o, onOpenChange: i, ...s} = e
      , [l=!0,a] = qs({
        prop: r,
        defaultProp: o,
        onChange: i
    });
    return m.jsx(nc, {
        present: n || l,
        children: m.jsx(vx, {
            open: l,
            ...s,
            ref: t,
            onClose: () => a(!1),
            onPause: at(e.onPause),
            onResume: at(e.onResume),
            onSwipeStart: le(e.onSwipeStart, u => {
                u.currentTarget.setAttribute("data-swipe", "start")
            }
            ),
            onSwipeMove: le(e.onSwipeMove, u => {
                const {x: d, y: f} = u.detail.delta;
                u.currentTarget.setAttribute("data-swipe", "move"),
                u.currentTarget.style.setProperty("--radix-toast-swipe-move-x", `${d}px`),
                u.currentTarget.style.setProperty("--radix-toast-swipe-move-y", `${f}px`)
            }
            ),
            onSwipeCancel: le(e.onSwipeCancel, u => {
                u.currentTarget.setAttribute("data-swipe", "cancel"),
                u.currentTarget.style.removeProperty("--radix-toast-swipe-move-x"),
                u.currentTarget.style.removeProperty("--radix-toast-swipe-move-y"),
                u.currentTarget.style.removeProperty("--radix-toast-swipe-end-x"),
                u.currentTarget.style.removeProperty("--radix-toast-swipe-end-y")
            }
            ),
            onSwipeEnd: le(e.onSwipeEnd, u => {
                const {x: d, y: f} = u.detail.delta;
                u.currentTarget.setAttribute("data-swipe", "end"),
                u.currentTarget.style.removeProperty("--radix-toast-swipe-move-x"),
                u.currentTarget.style.removeProperty("--radix-toast-swipe-move-y"),
                u.currentTarget.style.setProperty("--radix-toast-swipe-end-x", `${d}px`),
                u.currentTarget.style.setProperty("--radix-toast-swipe-end-y", `${f}px`),
                a(!1)
            }
            )
        })
    })
}
);
am.displayName = el;
var [hx,mx] = rm(el, {
    onClose() {}
})
  , vx = g.forwardRef( (e, t) => {
    const {__scopeToast: n, type: r="foreground", duration: o, open: i, onClose: s, onEscapeKeyDown: l, onPause: a, onResume: u, onSwipeStart: d, onSwipeMove: f, onSwipeCancel: c, onSwipeEnd: y, ...w} = e
      , x = Js(el, n)
      , [E,h] = g.useState(null)
      , p = Oe(t, L => h(L))
      , v = g.useRef(null)
      , S = g.useRef(null)
      , C = o || x.duration
      , P = g.useRef(0)
      , b = g.useRef(C)
      , N = g.useRef(0)
      , {onToastAdd: _, onToastRemove: O} = x
      , $ = at( () => {
        var Q;
        (E == null ? void 0 : E.contains(document.activeElement)) && ((Q = x.viewport) == null || Q.focus()),
        s()
    }
    )
      , D = g.useCallback(L => {
        !L || L === 1 / 0 || (window.clearTimeout(N.current),
        P.current = new Date().getTime(),
        N.current = window.setTimeout($, L))
    }
    , [$]);
    g.useEffect( () => {
        const L = x.viewport;
        if (L) {
            const Q = () => {
                D(b.current),
                u == null || u()
            }
              , U = () => {
                const K = new Date().getTime() - P.current;
                b.current = b.current - K,
                window.clearTimeout(N.current),
                a == null || a()
            }
            ;
            return L.addEventListener(Ha, U),
            L.addEventListener(Qa, Q),
            () => {
                L.removeEventListener(Ha, U),
                L.removeEventListener(Qa, Q)
            }
        }
    }
    , [x.viewport, C, a, u, D]),
    g.useEffect( () => {
        i && !x.isClosePausedRef.current && D(C)
    }
    , [i, C, x.isClosePausedRef, D]),
    g.useEffect( () => (_(),
    () => O()), [_, O]);
    const H = g.useMemo( () => E ? mm(E) : null, [E]);
    return x.viewport ? m.jsxs(m.Fragment, {
        children: [H && m.jsx(gx, {
            __scopeToast: n,
            role: "status",
            "aria-live": r === "foreground" ? "assertive" : "polite",
            "aria-atomic": !0,
            children: H
        }), m.jsx(hx, {
            scope: n,
            onClose: $,
            children: Jr.createPortal(m.jsx(oc.ItemSlot, {
                scope: n,
                children: m.jsx(q0, {
                    asChild: !0,
                    onEscapeKeyDown: le(l, () => {
                        x.isFocusedToastEscapeKeyDownRef.current || $(),
                        x.isFocusedToastEscapeKeyDownRef.current = !1
                    }
                    ),
                    children: m.jsx(me.li, {
                        role: "status",
                        "aria-live": "off",
                        "aria-atomic": !0,
                        tabIndex: 0,
                        "data-state": i ? "open" : "closed",
                        "data-swipe-direction": x.swipeDirection,
                        ...w,
                        ref: p,
                        style: {
                            userSelect: "none",
                            touchAction: "none",
                            ...e.style
                        },
                        onKeyDown: le(e.onKeyDown, L => {
                            L.key === "Escape" && (l == null || l(L.nativeEvent),
                            L.nativeEvent.defaultPrevented || (x.isFocusedToastEscapeKeyDownRef.current = !0,
                            $()))
                        }
                        ),
                        onPointerDown: le(e.onPointerDown, L => {
                            L.button === 0 && (v.current = {
                                x: L.clientX,
                                y: L.clientY
                            })
                        }
                        ),
                        onPointerMove: le(e.onPointerMove, L => {
                            if (!v.current)
                                return;
                            const Q = L.clientX - v.current.x
                              , U = L.clientY - v.current.y
                              , K = !!S.current
                              , k = ["left", "right"].includes(x.swipeDirection)
                              , j = ["left", "up"].includes(x.swipeDirection) ? Math.min : Math.max
                              , z = k ? j(0, Q) : 0
                              , I = k ? 0 : j(0, U)
                              , F = L.pointerType === "touch" ? 10 : 2
                              , Y = {
                                x: z,
                                y: I
                            }
                              , ae = {
                                originalEvent: L,
                                delta: Y
                            };
                            K ? (S.current = Y,
                            Di(dx, f, ae, {
                                discrete: !1
                            })) : Vd(Y, x.swipeDirection, F) ? (S.current = Y,
                            Di(cx, d, ae, {
                                discrete: !1
                            }),
                            L.target.setPointerCapture(L.pointerId)) : (Math.abs(Q) > F || Math.abs(U) > F) && (v.current = null)
                        }
                        ),
                        onPointerUp: le(e.onPointerUp, L => {
                            const Q = S.current
                              , U = L.target;
                            if (U.hasPointerCapture(L.pointerId) && U.releasePointerCapture(L.pointerId),
                            S.current = null,
                            v.current = null,
                            Q) {
                                const K = L.currentTarget
                                  , k = {
                                    originalEvent: L,
                                    delta: Q
                                };
                                Vd(Q, x.swipeDirection, x.swipeThreshold) ? Di(px, y, k, {
                                    discrete: !0
                                }) : Di(fx, c, k, {
                                    discrete: !0
                                }),
                                K.addEventListener("click", j => j.preventDefault(), {
                                    once: !0
                                })
                            }
                        }
                        )
                    })
                })
            }), x.viewport)
        })]
    }) : null
}
)
  , gx = e => {
    const {__scopeToast: t, children: n, ...r} = e
      , o = Js(el, t)
      , [i,s] = g.useState(!1)
      , [l,a] = g.useState(!1);
    return wx( () => s(!0)),
    g.useEffect( () => {
        const u = window.setTimeout( () => a(!0), 1e3);
        return () => window.clearTimeout(u)
    }
    , []),
    l ? null : m.jsx(nm, {
        asChild: !0,
        children: m.jsx(Zs, {
            ...r,
            children: i && m.jsxs(m.Fragment, {
                children: [o.label, " ", n]
            })
        })
    })
}
  , yx = "ToastTitle"
  , um = g.forwardRef( (e, t) => {
    const {__scopeToast: n, ...r} = e;
    return m.jsx(me.div, {
        ...r,
        ref: t
    })
}
);
um.displayName = yx;
var xx = "ToastDescription"
  , cm = g.forwardRef( (e, t) => {
    const {__scopeToast: n, ...r} = e;
    return m.jsx(me.div, {
        ...r,
        ref: t
    })
}
);
cm.displayName = xx;
var dm = "ToastAction"
  , fm = g.forwardRef( (e, t) => {
    const {altText: n, ...r} = e;
    return n.trim() ? m.jsx(hm, {
        altText: n,
        asChild: !0,
        children: m.jsx(ic, {
            ...r,
            ref: t
        })
    }) : (console.error(`Invalid prop \`altText\` supplied to \`${dm}\`. Expected non-empty \`string\`.`),
    null)
}
);
fm.displayName = dm;
var pm = "ToastClose"
  , ic = g.forwardRef( (e, t) => {
    const {__scopeToast: n, ...r} = e
      , o = mx(pm, n);
    return m.jsx(hm, {
        asChild: !0,
        children: m.jsx(me.button, {
            type: "button",
            ...r,
            ref: t,
            onClick: le(e.onClick, o.onClose)
        })
    })
}
);
ic.displayName = pm;
var hm = g.forwardRef( (e, t) => {
    const {__scopeToast: n, altText: r, ...o} = e;
    return m.jsx(me.div, {
        "data-radix-toast-announce-exclude": "",
        "data-radix-toast-announce-alt": r || void 0,
        ...o,
        ref: t
    })
}
);
function mm(e) {
    const t = [];
    return Array.from(e.childNodes).forEach(r => {
        if (r.nodeType === r.TEXT_NODE && r.textContent && t.push(r.textContent),
        Ex(r)) {
            const o = r.ariaHidden || r.hidden || r.style.display === "none"
              , i = r.dataset.radixToastAnnounceExclude === "";
            if (!o)
                if (i) {
                    const s = r.dataset.radixToastAnnounceAlt;
                    s && t.push(s)
                } else
                    t.push(...mm(r))
        }
    }
    ),
    t
}
function Di(e, t, n, {discrete: r}) {
    const o = n.originalEvent.currentTarget
      , i = new CustomEvent(e,{
        bubbles: !0,
        cancelable: !0,
        detail: n
    });
    t && o.addEventListener(e, t, {
        once: !0
    }),
    r ? tc(o, i) : o.dispatchEvent(i)
}
var Vd = (e, t, n=0) => {
    const r = Math.abs(e.x)
      , o = Math.abs(e.y)
      , i = r > o;
    return t === "left" || t === "right" ? i && r > n : !i && o > n
}
;
function wx(e= () => {}
) {
    const t = at(e);
    xt( () => {
        let n = 0
          , r = 0;
        return n = window.requestAnimationFrame( () => r = window.requestAnimationFrame(t)),
        () => {
            window.cancelAnimationFrame(n),
            window.cancelAnimationFrame(r)
        }
    }
    , [t])
}
function Ex(e) {
    return e.nodeType === e.ELEMENT_NODE
}
function Sx(e) {
    const t = []
      , n = document.createTreeWalker(e, NodeFilter.SHOW_ELEMENT, {
        acceptNode: r => {
            const o = r.tagName === "INPUT" && r.type === "hidden";
            return r.disabled || r.hidden || o ? NodeFilter.FILTER_SKIP : r.tabIndex >= 0 ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP
        }
    });
    for (; n.nextNode(); )
        t.push(n.currentNode);
    return t
}
function Hl(e) {
    const t = document.activeElement;
    return e.some(n => n === t ? !0 : (n.focus(),
    document.activeElement !== t))
}
var Cx = om
  , vm = sm
  , gm = am
  , ym = um
  , xm = cm
  , wm = fm
  , Em = ic;
function Sm(e) {
    var t, n, r = "";
    if (typeof e == "string" || typeof e == "number")
        r += e;
    else if (typeof e == "object")
        if (Array.isArray(e)) {
            var o = e.length;
            for (t = 0; t < o; t++)
                e[t] && (n = Sm(e[t])) && (r && (r += " "),
                r += n)
        } else
            for (n in e)
                e[n] && (r && (r += " "),
                r += n);
    return r
}
function Cm() {
    for (var e, t, n = 0, r = "", o = arguments.length; n < o; n++)
        (e = arguments[n]) && (t = Sm(e)) && (r && (r += " "),
        r += t);
    return r
}
const Hd = e => typeof e == "boolean" ? `${e}` : e === 0 ? "0" : e
  , Qd = Cm
  , bm = (e, t) => n => {
    var r;
    if ((t == null ? void 0 : t.variants) == null)
        return Qd(e, n == null ? void 0 : n.class, n == null ? void 0 : n.className);
    const {variants: o, defaultVariants: i} = t
      , s = Object.keys(o).map(u => {
        const d = n == null ? void 0 : n[u]
          , f = i == null ? void 0 : i[u];
        if (d === null)
            return null;
        const c = Hd(d) || Hd(f);
        return o[u][c]
    }
    )
      , l = n && Object.entries(n).reduce( (u, d) => {
        let[f,c] = d;
        return c === void 0 || (u[f] = c),
        u
    }
    , {})
      , a = t == null || (r = t.compoundVariants) === null || r === void 0 ? void 0 : r.reduce( (u, d) => {
        let {class: f, className: c, ...y} = d;
        return Object.entries(y).every(w => {
            let[x,E] = w;
            return Array.isArray(E) ? E.includes({
                ...i,
                ...l
            }[x]) : {
                ...i,
                ...l
            }[x] === E
        }
        ) ? [...u, f, c] : u
    }
    , []);
    return Qd(e, s, a, n == null ? void 0 : n.class, n == null ? void 0 : n.className)
}
;
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const bx = e => e.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase()
  , km = (...e) => e.filter( (t, n, r) => !!t && t.trim() !== "" && r.indexOf(t) === n).join(" ").trim();
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
var kx = {
    xmlns: "http://www.w3.org/2000/svg",
    width: 24,
    height: 24,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 2,
    strokeLinecap: "round",
    strokeLinejoin: "round"
};
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Px = g.forwardRef( ({color: e="currentColor", size: t=24, strokeWidth: n=2, absoluteStrokeWidth: r, className: o="", children: i, iconNode: s, ...l}, a) => g.createElement("svg", {
    ref: a,
    ...kx,
    width: t,
    height: t,
    stroke: e,
    strokeWidth: r ? Number(n) * 24 / Number(t) : n,
    className: km("lucide", o),
    ...l
}, [...s.map( ([u,d]) => g.createElement(u, d)), ...Array.isArray(i) ? i : [i]]));
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const St = (e, t) => {
    const n = g.forwardRef( ({className: r, ...o}, i) => g.createElement(Px, {
        ref: i,
        iconNode: t,
        className: km(`lucide-${bx(e)}`, r),
        ...o
    }));
    return n.displayName = `${e}`,
    n
}
;
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Nx = St("Award", [["path", {
    d: "m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526",
    key: "1yiouv"
}], ["circle", {
    cx: "12",
    cy: "8",
    r: "6",
    key: "1vp47v"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Tx = St("Check", [["path", {
    d: "M20 6 9 17l-5-5",
    key: "1gmf2c"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Rx = St("ChevronDown", [["path", {
    d: "m6 9 6 6 6-6",
    key: "qrunsl"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Ax = St("Gift", [["rect", {
    x: "3",
    y: "8",
    width: "18",
    height: "4",
    rx: "1",
    key: "bkv52"
}], ["path", {
    d: "M12 8v13",
    key: "1c76mn"
}], ["path", {
    d: "M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7",
    key: "6wjy6b"
}], ["path", {
    d: "M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5",
    key: "1ihvrl"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const sc = St("MessageCircle", [["path", {
    d: "M7.9 20A9 9 0 1 0 4 16.1L2 22Z",
    key: "vv11sd"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const jx = St("Shield", [["path", {
    d: "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
    key: "oel41y"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Ox = St("ShoppingCart", [["circle", {
    cx: "8",
    cy: "21",
    r: "1",
    key: "jimo8o"
}], ["circle", {
    cx: "19",
    cy: "21",
    r: "1",
    key: "13723u"
}], ["path", {
    d: "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12",
    key: "9zh506"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const vo = St("Star", [["path", {
    d: "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
    key: "r04s7s"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const _x = St("ThumbsUp", [["path", {
    d: "M7 10v12",
    key: "1qc93n"
}], ["path", {
    d: "M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z",
    key: "emmmcr"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Lx = St("X", [["path", {
    d: "M18 6 6 18",
    key: "1bl5f8"
}], ["path", {
    d: "m6 6 12 12",
    key: "d8bk6v"
}]]);
/**
 * @license lucide-react v0.462.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */
const Mx = St("Zap", [["path", {
    d: "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z",
    key: "1xq2db"
}]])
  , lc = "-"
  , Ix = e => {
    const t = zx(e)
      , {conflictingClassGroups: n, conflictingClassGroupModifiers: r} = e;
    return {
        getClassGroupId: s => {
            const l = s.split(lc);
            return l[0] === "" && l.length !== 1 && l.shift(),
            Pm(l, t) || Dx(s)
        }
        ,
        getConflictingClassGroupIds: (s, l) => {
            const a = n[s] || [];
            return l && r[s] ? [...a, ...r[s]] : a
        }
    }
}
  , Pm = (e, t) => {
    var s;
    if (e.length === 0)
        return t.classGroupId;
    const n = e[0]
      , r = t.nextPart.get(n)
      , o = r ? Pm(e.slice(1), r) : void 0;
    if (o)
        return o;
    if (t.validators.length === 0)
        return;
    const i = e.join(lc);
    return (s = t.validators.find( ({validator: l}) => l(i))) == null ? void 0 : s.classGroupId
}
  , Kd = /^\[(.+)\]$/
  , Dx = e => {
    if (Kd.test(e)) {
        const t = Kd.exec(e)[1]
          , n = t == null ? void 0 : t.substring(0, t.indexOf(":"));
        if (n)
            return "arbitrary.." + n
    }
}
  , zx = e => {
    const {theme: t, prefix: n} = e
      , r = {
        nextPart: new Map,
        validators: []
    };
    return $x(Object.entries(e.classGroups), n).forEach( ([i,s]) => {
        Ga(s, r, i, t)
    }
    ),
    r
}
  , Ga = (e, t, n, r) => {
    e.forEach(o => {
        if (typeof o == "string") {
            const i = o === "" ? t : Gd(t, o);
            i.classGroupId = n;
            return
        }
        if (typeof o == "function") {
            if (Fx(o)) {
                Ga(o(r), t, n, r);
                return
            }
            t.validators.push({
                validator: o,
                classGroupId: n
            });
            return
        }
        Object.entries(o).forEach( ([i,s]) => {
            Ga(s, Gd(t, i), n, r)
        }
        )
    }
    )
}
  , Gd = (e, t) => {
    let n = e;
    return t.split(lc).forEach(r => {
        n.nextPart.has(r) || n.nextPart.set(r, {
            nextPart: new Map,
            validators: []
        }),
        n = n.nextPart.get(r)
    }
    ),
    n
}
  , Fx = e => e.isThemeGetter
  , $x = (e, t) => t ? e.map( ([n,r]) => {
    const o = r.map(i => typeof i == "string" ? t + i : typeof i == "object" ? Object.fromEntries(Object.entries(i).map( ([s,l]) => [t + s, l])) : i);
    return [n, o]
}
) : e
  , Ux = e => {
    if (e < 1)
        return {
            get: () => {}
            ,
            set: () => {}
        };
    let t = 0
      , n = new Map
      , r = new Map;
    const o = (i, s) => {
        n.set(i, s),
        t++,
        t > e && (t = 0,
        r = n,
        n = new Map)
    }
    ;
    return {
        get(i) {
            let s = n.get(i);
            if (s !== void 0)
                return s;
            if ((s = r.get(i)) !== void 0)
                return o(i, s),
                s
        },
        set(i, s) {
            n.has(i) ? n.set(i, s) : o(i, s)
        }
    }
}
  , Nm = "!"
  , Bx = e => {
    const {separator: t, experimentalParseClassName: n} = e
      , r = t.length === 1
      , o = t[0]
      , i = t.length
      , s = l => {
        const a = [];
        let u = 0, d = 0, f;
        for (let E = 0; E < l.length; E++) {
            let h = l[E];
            if (u === 0) {
                if (h === o && (r || l.slice(E, E + i) === t)) {
                    a.push(l.slice(d, E)),
                    d = E + i;
                    continue
                }
                if (h === "/") {
                    f = E;
                    continue
                }
            }
            h === "[" ? u++ : h === "]" && u--
        }
        const c = a.length === 0 ? l : l.substring(d)
          , y = c.startsWith(Nm)
          , w = y ? c.substring(1) : c
          , x = f && f > d ? f - d : void 0;
        return {
            modifiers: a,
            hasImportantModifier: y,
            baseClassName: w,
            maybePostfixModifierPosition: x
        }
    }
    ;
    return n ? l => n({
        className: l,
        parseClassName: s
    }) : s
}
  , Wx = e => {
    if (e.length <= 1)
        return e;
    const t = [];
    let n = [];
    return e.forEach(r => {
        r[0] === "[" ? (t.push(...n.sort(), r),
        n = []) : n.push(r)
    }
    ),
    t.push(...n.sort()),
    t
}
  , Vx = e => ({
    cache: Ux(e.cacheSize),
    parseClassName: Bx(e),
    ...Ix(e)
})
  , Hx = /\s+/
  , Qx = (e, t) => {
    const {parseClassName: n, getClassGroupId: r, getConflictingClassGroupIds: o} = t
      , i = []
      , s = e.trim().split(Hx);
    let l = "";
    for (let a = s.length - 1; a >= 0; a -= 1) {
        const u = s[a]
          , {modifiers: d, hasImportantModifier: f, baseClassName: c, maybePostfixModifierPosition: y} = n(u);
        let w = !!y
          , x = r(w ? c.substring(0, y) : c);
        if (!x) {
            if (!w) {
                l = u + (l.length > 0 ? " " + l : l);
                continue
            }
            if (x = r(c),
            !x) {
                l = u + (l.length > 0 ? " " + l : l);
                continue
            }
            w = !1
        }
        const E = Wx(d).join(":")
          , h = f ? E + Nm : E
          , p = h + x;
        if (i.includes(p))
            continue;
        i.push(p);
        const v = o(x, w);
        for (let S = 0; S < v.length; ++S) {
            const C = v[S];
            i.push(h + C)
        }
        l = u + (l.length > 0 ? " " + l : l)
    }
    return l
}
;
function Kx() {
    let e = 0, t, n, r = "";
    for (; e < arguments.length; )
        (t = arguments[e++]) && (n = Tm(t)) && (r && (r += " "),
        r += n);
    return r
}
const Tm = e => {
    if (typeof e == "string")
        return e;
    let t, n = "";
    for (let r = 0; r < e.length; r++)
        e[r] && (t = Tm(e[r])) && (n && (n += " "),
        n += t);
    return n
}
;
function Gx(e, ...t) {
    let n, r, o, i = s;
    function s(a) {
        const u = t.reduce( (d, f) => f(d), e());
        return n = Vx(u),
        r = n.cache.get,
        o = n.cache.set,
        i = l,
        l(a)
    }
    function l(a) {
        const u = r(a);
        if (u)
            return u;
        const d = Qx(a, n);
        return o(a, d),
        d
    }
    return function() {
        return i(Kx.apply(null, arguments))
    }
}
const re = e => {
    const t = n => n[e] || [];
    return t.isThemeGetter = !0,
    t
}
  , Rm = /^\[(?:([a-z-]+):)?(.+)\]$/i
  , Yx = /^\d+\/\d+$/
  , Xx = new Set(["px", "full", "screen"])
  , qx = /^(\d+(\.\d+)?)?(xs|sm|md|lg|xl)$/
  , Zx = /\d+(%|px|r?em|[sdl]?v([hwib]|min|max)|pt|pc|in|cm|mm|cap|ch|ex|r?lh|cq(w|h|i|b|min|max))|\b(calc|min|max|clamp)\(.+\)|^0$/
  , Jx = /^(rgba?|hsla?|hwb|(ok)?(lab|lch))\(.+\)$/
  , ew = /^(inset_)?-?((\d+)?\.?(\d+)[a-z]+|0)_-?((\d+)?\.?(\d+)[a-z]+|0)/
  , tw = /^(url|image|image-set|cross-fade|element|(repeating-)?(linear|radial|conic)-gradient)\(.+\)$/
  , It = e => Ar(e) || Xx.has(e) || Yx.test(e)
  , rn = e => eo(e, "length", uw)
  , Ar = e => !!e && !Number.isNaN(Number(e))
  , Ql = e => eo(e, "number", Ar)
  , go = e => !!e && Number.isInteger(Number(e))
  , nw = e => e.endsWith("%") && Ar(e.slice(0, -1))
  , W = e => Rm.test(e)
  , on = e => qx.test(e)
  , rw = new Set(["length", "size", "percentage"])
  , ow = e => eo(e, rw, Am)
  , iw = e => eo(e, "position", Am)
  , sw = new Set(["image", "url"])
  , lw = e => eo(e, sw, dw)
  , aw = e => eo(e, "", cw)
  , yo = () => !0
  , eo = (e, t, n) => {
    const r = Rm.exec(e);
    return r ? r[1] ? typeof t == "string" ? r[1] === t : t.has(r[1]) : n(r[2]) : !1
}
  , uw = e => Zx.test(e) && !Jx.test(e)
  , Am = () => !1
  , cw = e => ew.test(e)
  , dw = e => tw.test(e)
  , fw = () => {
    const e = re("colors")
      , t = re("spacing")
      , n = re("blur")
      , r = re("brightness")
      , o = re("borderColor")
      , i = re("borderRadius")
      , s = re("borderSpacing")
      , l = re("borderWidth")
      , a = re("contrast")
      , u = re("grayscale")
      , d = re("hueRotate")
      , f = re("invert")
      , c = re("gap")
      , y = re("gradientColorStops")
      , w = re("gradientColorStopPositions")
      , x = re("inset")
      , E = re("margin")
      , h = re("opacity")
      , p = re("padding")
      , v = re("saturate")
      , S = re("scale")
      , C = re("sepia")
      , P = re("skew")
      , b = re("space")
      , N = re("translate")
      , _ = () => ["auto", "contain", "none"]
      , O = () => ["auto", "hidden", "clip", "visible", "scroll"]
      , $ = () => ["auto", W, t]
      , D = () => [W, t]
      , H = () => ["", It, rn]
      , L = () => ["auto", Ar, W]
      , Q = () => ["bottom", "center", "left", "left-bottom", "left-top", "right", "right-bottom", "right-top", "top"]
      , U = () => ["solid", "dashed", "dotted", "double", "none"]
      , K = () => ["normal", "multiply", "screen", "overlay", "darken", "lighten", "color-dodge", "color-burn", "hard-light", "soft-light", "difference", "exclusion", "hue", "saturation", "color", "luminosity"]
      , k = () => ["start", "end", "center", "between", "around", "evenly", "stretch"]
      , j = () => ["", "0", W]
      , z = () => ["auto", "avoid", "all", "avoid-page", "page", "left", "right", "column"]
      , I = () => [Ar, W];
    return {
        cacheSize: 500,
        separator: ":",
        theme: {
            colors: [yo],
            spacing: [It, rn],
            blur: ["none", "", on, W],
            brightness: I(),
            borderColor: [e],
            borderRadius: ["none", "", "full", on, W],
            borderSpacing: D(),
            borderWidth: H(),
            contrast: I(),
            grayscale: j(),
            hueRotate: I(),
            invert: j(),
            gap: D(),
            gradientColorStops: [e],
            gradientColorStopPositions: [nw, rn],
            inset: $(),
            margin: $(),
            opacity: I(),
            padding: D(),
            saturate: I(),
            scale: I(),
            sepia: j(),
            skew: I(),
            space: D(),
            translate: D()
        },
        classGroups: {
            aspect: [{
                aspect: ["auto", "square", "video", W]
            }],
            container: ["container"],
            columns: [{
                columns: [on]
            }],
            "break-after": [{
                "break-after": z()
            }],
            "break-before": [{
                "break-before": z()
            }],
            "break-inside": [{
                "break-inside": ["auto", "avoid", "avoid-page", "avoid-column"]
            }],
            "box-decoration": [{
                "box-decoration": ["slice", "clone"]
            }],
            box: [{
                box: ["border", "content"]
            }],
            display: ["block", "inline-block", "inline", "flex", "inline-flex", "table", "inline-table", "table-caption", "table-cell", "table-column", "table-column-group", "table-footer-group", "table-header-group", "table-row-group", "table-row", "flow-root", "grid", "inline-grid", "contents", "list-item", "hidden"],
            float: [{
                float: ["right", "left", "none", "start", "end"]
            }],
            clear: [{
                clear: ["left", "right", "both", "none", "start", "end"]
            }],
            isolation: ["isolate", "isolation-auto"],
            "object-fit": [{
                object: ["contain", "cover", "fill", "none", "scale-down"]
            }],
            "object-position": [{
                object: [...Q(), W]
            }],
            overflow: [{
                overflow: O()
            }],
            "overflow-x": [{
                "overflow-x": O()
            }],
            "overflow-y": [{
                "overflow-y": O()
            }],
            overscroll: [{
                overscroll: _()
            }],
            "overscroll-x": [{
                "overscroll-x": _()
            }],
            "overscroll-y": [{
                "overscroll-y": _()
            }],
            position: ["static", "fixed", "absolute", "relative", "sticky"],
            inset: [{
                inset: [x]
            }],
            "inset-x": [{
                "inset-x": [x]
            }],
            "inset-y": [{
                "inset-y": [x]
            }],
            start: [{
                start: [x]
            }],
            end: [{
                end: [x]
            }],
            top: [{
                top: [x]
            }],
            right: [{
                right: [x]
            }],
            bottom: [{
                bottom: [x]
            }],
            left: [{
                left: [x]
            }],
            visibility: ["visible", "invisible", "collapse"],
            z: [{
                z: ["auto", go, W]
            }],
            basis: [{
                basis: $()
            }],
            "flex-direction": [{
                flex: ["row", "row-reverse", "col", "col-reverse"]
            }],
            "flex-wrap": [{
                flex: ["wrap", "wrap-reverse", "nowrap"]
            }],
            flex: [{
                flex: ["1", "auto", "initial", "none", W]
            }],
            grow: [{
                grow: j()
            }],
            shrink: [{
                shrink: j()
            }],
            order: [{
                order: ["first", "last", "none", go, W]
            }],
            "grid-cols": [{
                "grid-cols": [yo]
            }],
            "col-start-end": [{
                col: ["auto", {
                    span: ["full", go, W]
                }, W]
            }],
            "col-start": [{
                "col-start": L()
            }],
            "col-end": [{
                "col-end": L()
            }],
            "grid-rows": [{
                "grid-rows": [yo]
            }],
            "row-start-end": [{
                row: ["auto", {
                    span: [go, W]
                }, W]
            }],
            "row-start": [{
                "row-start": L()
            }],
            "row-end": [{
                "row-end": L()
            }],
            "grid-flow": [{
                "grid-flow": ["row", "col", "dense", "row-dense", "col-dense"]
            }],
            "auto-cols": [{
                "auto-cols": ["auto", "min", "max", "fr", W]
            }],
            "auto-rows": [{
                "auto-rows": ["auto", "min", "max", "fr", W]
            }],
            gap: [{
                gap: [c]
            }],
            "gap-x": [{
                "gap-x": [c]
            }],
            "gap-y": [{
                "gap-y": [c]
            }],
            "justify-content": [{
                justify: ["normal", ...k()]
            }],
            "justify-items": [{
                "justify-items": ["start", "end", "center", "stretch"]
            }],
            "justify-self": [{
                "justify-self": ["auto", "start", "end", "center", "stretch"]
            }],
            "align-content": [{
                content: ["normal", ...k(), "baseline"]
            }],
            "align-items": [{
                items: ["start", "end", "center", "baseline", "stretch"]
            }],
            "align-self": [{
                self: ["auto", "start", "end", "center", "stretch", "baseline"]
            }],
            "place-content": [{
                "place-content": [...k(), "baseline"]
            }],
            "place-items": [{
                "place-items": ["start", "end", "center", "baseline", "stretch"]
            }],
            "place-self": [{
                "place-self": ["auto", "start", "end", "center", "stretch"]
            }],
            p: [{
                p: [p]
            }],
            px: [{
                px: [p]
            }],
            py: [{
                py: [p]
            }],
            ps: [{
                ps: [p]
            }],
            pe: [{
                pe: [p]
            }],
            pt: [{
                pt: [p]
            }],
            pr: [{
                pr: [p]
            }],
            pb: [{
                pb: [p]
            }],
            pl: [{
                pl: [p]
            }],
            m: [{
                m: [E]
            }],
            mx: [{
                mx: [E]
            }],
            my: [{
                my: [E]
            }],
            ms: [{
                ms: [E]
            }],
            me: [{
                me: [E]
            }],
            mt: [{
                mt: [E]
            }],
            mr: [{
                mr: [E]
            }],
            mb: [{
                mb: [E]
            }],
            ml: [{
                ml: [E]
            }],
            "space-x": [{
                "space-x": [b]
            }],
            "space-x-reverse": ["space-x-reverse"],
            "space-y": [{
                "space-y": [b]
            }],
            "space-y-reverse": ["space-y-reverse"],
            w: [{
                w: ["auto", "min", "max", "fit", "svw", "lvw", "dvw", W, t]
            }],
            "min-w": [{
                "min-w": [W, t, "min", "max", "fit"]
            }],
            "max-w": [{
                "max-w": [W, t, "none", "full", "min", "max", "fit", "prose", {
                    screen: [on]
                }, on]
            }],
            h: [{
                h: [W, t, "auto", "min", "max", "fit", "svh", "lvh", "dvh"]
            }],
            "min-h": [{
                "min-h": [W, t, "min", "max", "fit", "svh", "lvh", "dvh"]
            }],
            "max-h": [{
                "max-h": [W, t, "min", "max", "fit", "svh", "lvh", "dvh"]
            }],
            size: [{
                size: [W, t, "auto", "min", "max", "fit"]
            }],
            "font-size": [{
                text: ["base", on, rn]
            }],
            "font-smoothing": ["antialiased", "subpixel-antialiased"],
            "font-style": ["italic", "not-italic"],
            "font-weight": [{
                font: ["thin", "extralight", "light", "normal", "medium", "semibold", "bold", "extrabold", "black", Ql]
            }],
            "font-family": [{
                font: [yo]
            }],
            "fvn-normal": ["normal-nums"],
            "fvn-ordinal": ["ordinal"],
            "fvn-slashed-zero": ["slashed-zero"],
            "fvn-figure": ["lining-nums", "oldstyle-nums"],
            "fvn-spacing": ["proportional-nums", "tabular-nums"],
            "fvn-fraction": ["diagonal-fractions", "stacked-fractons"],
            tracking: [{
                tracking: ["tighter", "tight", "normal", "wide", "wider", "widest", W]
            }],
            "line-clamp": [{
                "line-clamp": ["none", Ar, Ql]
            }],
            leading: [{
                leading: ["none", "tight", "snug", "normal", "relaxed", "loose", It, W]
            }],
            "list-image": [{
                "list-image": ["none", W]
            }],
            "list-style-type": [{
                list: ["none", "disc", "decimal", W]
            }],
            "list-style-position": [{
                list: ["inside", "outside"]
            }],
            "placeholder-color": [{
                placeholder: [e]
            }],
            "placeholder-opacity": [{
                "placeholder-opacity": [h]
            }],
            "text-alignment": [{
                text: ["left", "center", "right", "justify", "start", "end"]
            }],
            "text-color": [{
                text: [e]
            }],
            "text-opacity": [{
                "text-opacity": [h]
            }],
            "text-decoration": ["underline", "overline", "line-through", "no-underline"],
            "text-decoration-style": [{
                decoration: [...U(), "wavy"]
            }],
            "text-decoration-thickness": [{
                decoration: ["auto", "from-font", It, rn]
            }],
            "underline-offset": [{
                "underline-offset": ["auto", It, W]
            }],
            "text-decoration-color": [{
                decoration: [e]
            }],
            "text-transform": ["uppercase", "lowercase", "capitalize", "normal-case"],
            "text-overflow": ["truncate", "text-ellipsis", "text-clip"],
            "text-wrap": [{
                text: ["wrap", "nowrap", "balance", "pretty"]
            }],
            indent: [{
                indent: D()
            }],
            "vertical-align": [{
                align: ["baseline", "top", "middle", "bottom", "text-top", "text-bottom", "sub", "super", W]
            }],
            whitespace: [{
                whitespace: ["normal", "nowrap", "pre", "pre-line", "pre-wrap", "break-spaces"]
            }],
            break: [{
                break: ["normal", "words", "all", "keep"]
            }],
            hyphens: [{
                hyphens: ["none", "manual", "auto"]
            }],
            content: [{
                content: ["none", W]
            }],
            "bg-attachment": [{
                bg: ["fixed", "local", "scroll"]
            }],
            "bg-clip": [{
                "bg-clip": ["border", "padding", "content", "text"]
            }],
            "bg-opacity": [{
                "bg-opacity": [h]
            }],
            "bg-origin": [{
                "bg-origin": ["border", "padding", "content"]
            }],
            "bg-position": [{
                bg: [...Q(), iw]
            }],
            "bg-repeat": [{
                bg: ["no-repeat", {
                    repeat: ["", "x", "y", "round", "space"]
                }]
            }],
            "bg-size": [{
                bg: ["auto", "cover", "contain", ow]
            }],
            "bg-image": [{
                bg: ["none", {
                    "gradient-to": ["t", "tr", "r", "br", "b", "bl", "l", "tl"]
                }, lw]
            }],
            "bg-color": [{
                bg: [e]
            }],
            "gradient-from-pos": [{
                from: [w]
            }],
            "gradient-via-pos": [{
                via: [w]
            }],
            "gradient-to-pos": [{
                to: [w]
            }],
            "gradient-from": [{
                from: [y]
            }],
            "gradient-via": [{
                via: [y]
            }],
            "gradient-to": [{
                to: [y]
            }],
            rounded: [{
                rounded: [i]
            }],
            "rounded-s": [{
                "rounded-s": [i]
            }],
            "rounded-e": [{
                "rounded-e": [i]
            }],
            "rounded-t": [{
                "rounded-t": [i]
            }],
            "rounded-r": [{
                "rounded-r": [i]
            }],
            "rounded-b": [{
                "rounded-b": [i]
            }],
            "rounded-l": [{
                "rounded-l": [i]
            }],
            "rounded-ss": [{
                "rounded-ss": [i]
            }],
            "rounded-se": [{
                "rounded-se": [i]
            }],
            "rounded-ee": [{
                "rounded-ee": [i]
            }],
            "rounded-es": [{
                "rounded-es": [i]
            }],
            "rounded-tl": [{
                "rounded-tl": [i]
            }],
            "rounded-tr": [{
                "rounded-tr": [i]
            }],
            "rounded-br": [{
                "rounded-br": [i]
            }],
            "rounded-bl": [{
                "rounded-bl": [i]
            }],
            "border-w": [{
                border: [l]
            }],
            "border-w-x": [{
                "border-x": [l]
            }],
            "border-w-y": [{
                "border-y": [l]
            }],
            "border-w-s": [{
                "border-s": [l]
            }],
            "border-w-e": [{
                "border-e": [l]
            }],
            "border-w-t": [{
                "border-t": [l]
            }],
            "border-w-r": [{
                "border-r": [l]
            }],
            "border-w-b": [{
                "border-b": [l]
            }],
            "border-w-l": [{
                "border-l": [l]
            }],
            "border-opacity": [{
                "border-opacity": [h]
            }],
            "border-style": [{
                border: [...U(), "hidden"]
            }],
            "divide-x": [{
                "divide-x": [l]
            }],
            "divide-x-reverse": ["divide-x-reverse"],
            "divide-y": [{
                "divide-y": [l]
            }],
            "divide-y-reverse": ["divide-y-reverse"],
            "divide-opacity": [{
                "divide-opacity": [h]
            }],
            "divide-style": [{
                divide: U()
            }],
            "border-color": [{
                border: [o]
            }],
            "border-color-x": [{
                "border-x": [o]
            }],
            "border-color-y": [{
                "border-y": [o]
            }],
            "border-color-t": [{
                "border-t": [o]
            }],
            "border-color-r": [{
                "border-r": [o]
            }],
            "border-color-b": [{
                "border-b": [o]
            }],
            "border-color-l": [{
                "border-l": [o]
            }],
            "divide-color": [{
                divide: [o]
            }],
            "outline-style": [{
                outline: ["", ...U()]
            }],
            "outline-offset": [{
                "outline-offset": [It, W]
            }],
            "outline-w": [{
                outline: [It, rn]
            }],
            "outline-color": [{
                outline: [e]
            }],
            "ring-w": [{
                ring: H()
            }],
            "ring-w-inset": ["ring-inset"],
            "ring-color": [{
                ring: [e]
            }],
            "ring-opacity": [{
                "ring-opacity": [h]
            }],
            "ring-offset-w": [{
                "ring-offset": [It, rn]
            }],
            "ring-offset-color": [{
                "ring-offset": [e]
            }],
            shadow: [{
                shadow: ["", "inner", "none", on, aw]
            }],
            "shadow-color": [{
                shadow: [yo]
            }],
            opacity: [{
                opacity: [h]
            }],
            "mix-blend": [{
                "mix-blend": [...K(), "plus-lighter", "plus-darker"]
            }],
            "bg-blend": [{
                "bg-blend": K()
            }],
            filter: [{
                filter: ["", "none"]
            }],
            blur: [{
                blur: [n]
            }],
            brightness: [{
                brightness: [r]
            }],
            contrast: [{
                contrast: [a]
            }],
            "drop-shadow": [{
                "drop-shadow": ["", "none", on, W]
            }],
            grayscale: [{
                grayscale: [u]
            }],
            "hue-rotate": [{
                "hue-rotate": [d]
            }],
            invert: [{
                invert: [f]
            }],
            saturate: [{
                saturate: [v]
            }],
            sepia: [{
                sepia: [C]
            }],
            "backdrop-filter": [{
                "backdrop-filter": ["", "none"]
            }],
            "backdrop-blur": [{
                "backdrop-blur": [n]
            }],
            "backdrop-brightness": [{
                "backdrop-brightness": [r]
            }],
            "backdrop-contrast": [{
                "backdrop-contrast": [a]
            }],
            "backdrop-grayscale": [{
                "backdrop-grayscale": [u]
            }],
            "backdrop-hue-rotate": [{
                "backdrop-hue-rotate": [d]
            }],
            "backdrop-invert": [{
                "backdrop-invert": [f]
            }],
            "backdrop-opacity": [{
                "backdrop-opacity": [h]
            }],
            "backdrop-saturate": [{
                "backdrop-saturate": [v]
            }],
            "backdrop-sepia": [{
                "backdrop-sepia": [C]
            }],
            "border-collapse": [{
                border: ["collapse", "separate"]
            }],
            "border-spacing": [{
                "border-spacing": [s]
            }],
            "border-spacing-x": [{
                "border-spacing-x": [s]
            }],
            "border-spacing-y": [{
                "border-spacing-y": [s]
            }],
            "table-layout": [{
                table: ["auto", "fixed"]
            }],
            caption: [{
                caption: ["top", "bottom"]
            }],
            transition: [{
                transition: ["none", "all", "", "colors", "opacity", "shadow", "transform", W]
            }],
            duration: [{
                duration: I()
            }],
            ease: [{
                ease: ["linear", "in", "out", "in-out", W]
            }],
            delay: [{
                delay: I()
            }],
            animate: [{
                animate: ["none", "spin", "ping", "pulse", "bounce", W]
            }],
            transform: [{
                transform: ["", "gpu", "none"]
            }],
            scale: [{
                scale: [S]
            }],
            "scale-x": [{
                "scale-x": [S]
            }],
            "scale-y": [{
                "scale-y": [S]
            }],
            rotate: [{
                rotate: [go, W]
            }],
            "translate-x": [{
                "translate-x": [N]
            }],
            "translate-y": [{
                "translate-y": [N]
            }],
            "skew-x": [{
                "skew-x": [P]
            }],
            "skew-y": [{
                "skew-y": [P]
            }],
            "transform-origin": [{
                origin: ["center", "top", "top-right", "right", "bottom-right", "bottom", "bottom-left", "left", "top-left", W]
            }],
            accent: [{
                accent: ["auto", e]
            }],
            appearance: [{
                appearance: ["none", "auto"]
            }],
            cursor: [{
                cursor: ["auto", "default", "pointer", "wait", "text", "move", "help", "not-allowed", "none", "context-menu", "progress", "cell", "crosshair", "vertical-text", "alias", "copy", "no-drop", "grab", "grabbing", "all-scroll", "col-resize", "row-resize", "n-resize", "e-resize", "s-resize", "w-resize", "ne-resize", "nw-resize", "se-resize", "sw-resize", "ew-resize", "ns-resize", "nesw-resize", "nwse-resize", "zoom-in", "zoom-out", W]
            }],
            "caret-color": [{
                caret: [e]
            }],
            "pointer-events": [{
                "pointer-events": ["none", "auto"]
            }],
            resize: [{
                resize: ["none", "y", "x", ""]
            }],
            "scroll-behavior": [{
                scroll: ["auto", "smooth"]
            }],
            "scroll-m": [{
                "scroll-m": D()
            }],
            "scroll-mx": [{
                "scroll-mx": D()
            }],
            "scroll-my": [{
                "scroll-my": D()
            }],
            "scroll-ms": [{
                "scroll-ms": D()
            }],
            "scroll-me": [{
                "scroll-me": D()
            }],
            "scroll-mt": [{
                "scroll-mt": D()
            }],
            "scroll-mr": [{
                "scroll-mr": D()
            }],
            "scroll-mb": [{
                "scroll-mb": D()
            }],
            "scroll-ml": [{
                "scroll-ml": D()
            }],
            "scroll-p": [{
                "scroll-p": D()
            }],
            "scroll-px": [{
                "scroll-px": D()
            }],
            "scroll-py": [{
                "scroll-py": D()
            }],
            "scroll-ps": [{
                "scroll-ps": D()
            }],
            "scroll-pe": [{
                "scroll-pe": D()
            }],
            "scroll-pt": [{
                "scroll-pt": D()
            }],
            "scroll-pr": [{
                "scroll-pr": D()
            }],
            "scroll-pb": [{
                "scroll-pb": D()
            }],
            "scroll-pl": [{
                "scroll-pl": D()
            }],
            "snap-align": [{
                snap: ["start", "end", "center", "align-none"]
            }],
            "snap-stop": [{
                snap: ["normal", "always"]
            }],
            "snap-type": [{
                snap: ["none", "x", "y", "both"]
            }],
            "snap-strictness": [{
                snap: ["mandatory", "proximity"]
            }],
            touch: [{
                touch: ["auto", "none", "manipulation"]
            }],
            "touch-x": [{
                "touch-pan": ["x", "left", "right"]
            }],
            "touch-y": [{
                "touch-pan": ["y", "up", "down"]
            }],
            "touch-pz": ["touch-pinch-zoom"],
            select: [{
                select: ["none", "text", "all", "auto"]
            }],
            "will-change": [{
                "will-change": ["auto", "scroll", "contents", "transform", W]
            }],
            fill: [{
                fill: [e, "none"]
            }],
            "stroke-w": [{
                stroke: [It, rn, Ql]
            }],
            stroke: [{
                stroke: [e, "none"]
            }],
            sr: ["sr-only", "not-sr-only"],
            "forced-color-adjust": [{
                "forced-color-adjust": ["auto", "none"]
            }]
        },
        conflictingClassGroups: {
            overflow: ["overflow-x", "overflow-y"],
            overscroll: ["overscroll-x", "overscroll-y"],
            inset: ["inset-x", "inset-y", "start", "end", "top", "right", "bottom", "left"],
            "inset-x": ["right", "left"],
            "inset-y": ["top", "bottom"],
            flex: ["basis", "grow", "shrink"],
            gap: ["gap-x", "gap-y"],
            p: ["px", "py", "ps", "pe", "pt", "pr", "pb", "pl"],
            px: ["pr", "pl"],
            py: ["pt", "pb"],
            m: ["mx", "my", "ms", "me", "mt", "mr", "mb", "ml"],
            mx: ["mr", "ml"],
            my: ["mt", "mb"],
            size: ["w", "h"],
            "font-size": ["leading"],
            "fvn-normal": ["fvn-ordinal", "fvn-slashed-zero", "fvn-figure", "fvn-spacing", "fvn-fraction"],
            "fvn-ordinal": ["fvn-normal"],
            "fvn-slashed-zero": ["fvn-normal"],
            "fvn-figure": ["fvn-normal"],
            "fvn-spacing": ["fvn-normal"],
            "fvn-fraction": ["fvn-normal"],
            "line-clamp": ["display", "overflow"],
            rounded: ["rounded-s", "rounded-e", "rounded-t", "rounded-r", "rounded-b", "rounded-l", "rounded-ss", "rounded-se", "rounded-ee", "rounded-es", "rounded-tl", "rounded-tr", "rounded-br", "rounded-bl"],
            "rounded-s": ["rounded-ss", "rounded-es"],
            "rounded-e": ["rounded-se", "rounded-ee"],
            "rounded-t": ["rounded-tl", "rounded-tr"],
            "rounded-r": ["rounded-tr", "rounded-br"],
            "rounded-b": ["rounded-br", "rounded-bl"],
            "rounded-l": ["rounded-tl", "rounded-bl"],
            "border-spacing": ["border-spacing-x", "border-spacing-y"],
            "border-w": ["border-w-s", "border-w-e", "border-w-t", "border-w-r", "border-w-b", "border-w-l"],
            "border-w-x": ["border-w-r", "border-w-l"],
            "border-w-y": ["border-w-t", "border-w-b"],
            "border-color": ["border-color-t", "border-color-r", "border-color-b", "border-color-l"],
            "border-color-x": ["border-color-r", "border-color-l"],
            "border-color-y": ["border-color-t", "border-color-b"],
            "scroll-m": ["scroll-mx", "scroll-my", "scroll-ms", "scroll-me", "scroll-mt", "scroll-mr", "scroll-mb", "scroll-ml"],
            "scroll-mx": ["scroll-mr", "scroll-ml"],
            "scroll-my": ["scroll-mt", "scroll-mb"],
            "scroll-p": ["scroll-px", "scroll-py", "scroll-ps", "scroll-pe", "scroll-pt", "scroll-pr", "scroll-pb", "scroll-pl"],
            "scroll-px": ["scroll-pr", "scroll-pl"],
            "scroll-py": ["scroll-pt", "scroll-pb"],
            touch: ["touch-x", "touch-y", "touch-pz"],
            "touch-x": ["touch"],
            "touch-y": ["touch"],
            "touch-pz": ["touch"]
        },
        conflictingClassGroupModifiers: {
            "font-size": ["leading"]
        }
    }
}
  , pw = Gx(fw);
function Ct(...e) {
    return pw(Cm(e))
}
const hw = Cx
  , jm = g.forwardRef( ({className: e, ...t}, n) => m.jsx(vm, {
    ref: n,
    className: Ct("fixed top-0 z-[100] flex max-h-screen w-full flex-col-reverse p-4 sm:bottom-0 sm:right-0 sm:top-auto sm:flex-col md:max-w-[420px]", e),
    ...t
}));
jm.displayName = vm.displayName;
const mw = bm("group pointer-events-auto relative flex w-full items-center justify-between space-x-4 overflow-hidden rounded-md border p-6 pr-8 shadow-lg transition-all data-[swipe=cancel]:translate-x-0 data-[swipe=end]:translate-x-[var(--radix-toast-swipe-end-x)] data-[swipe=move]:translate-x-[var(--radix-toast-swipe-move-x)] data-[swipe=move]:transition-none data-[state=open]:animate-in data-[state=closed]:animate-out data-[swipe=end]:animate-out data-[state=closed]:fade-out-80 data-[state=closed]:slide-out-to-right-full data-[state=open]:slide-in-from-top-full data-[state=open]:sm:slide-in-from-bottom-full", {
    variants: {
        variant: {
            default: "border bg-background text-foreground",
            destructive: "destructive group border-destructive bg-destructive text-destructive-foreground"
        }
    },
    defaultVariants: {
        variant: "default"
    }
})
  , Om = g.forwardRef( ({className: e, variant: t, ...n}, r) => m.jsx(gm, {
    ref: r,
    className: Ct(mw({
        variant: t
    }), e),
    ...n
}));
Om.displayName = gm.displayName;
const vw = g.forwardRef( ({className: e, ...t}, n) => m.jsx(wm, {
    ref: n,
    className: Ct("inline-flex h-8 shrink-0 items-center justify-center rounded-md border bg-transparent px-3 text-sm font-medium ring-offset-background transition-colors hover:bg-secondary focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 group-[.destructive]:border-muted/40 group-[.destructive]:hover:border-destructive/30 group-[.destructive]:hover:bg-destructive group-[.destructive]:hover:text-destructive-foreground group-[.destructive]:focus:ring-destructive", e),
    ...t
}));
vw.displayName = wm.displayName;
const _m = g.forwardRef( ({className: e, ...t}, n) => m.jsx(Em, {
    ref: n,
    className: Ct("absolute right-2 top-2 rounded-md p-1 text-foreground/50 opacity-0 transition-opacity hover:text-foreground focus:opacity-100 focus:outline-none focus:ring-2 group-hover:opacity-100 group-[.destructive]:text-red-300 group-[.destructive]:hover:text-red-50 group-[.destructive]:focus:ring-red-400 group-[.destructive]:focus:ring-offset-red-600", e),
    "toast-close": "",
    ...t,
    children: m.jsx(Lx, {
        className: "h-4 w-4"
    })
}));
_m.displayName = Em.displayName;
const Lm = g.forwardRef( ({className: e, ...t}, n) => m.jsx(ym, {
    ref: n,
    className: Ct("text-sm font-semibold", e),
    ...t
}));
Lm.displayName = ym.displayName;
const Mm = g.forwardRef( ({className: e, ...t}, n) => m.jsx(xm, {
    ref: n,
    className: Ct("text-sm opacity-90", e),
    ...t
}));
Mm.displayName = xm.displayName;
function gw() {
    const {toasts: e} = z0();
    return m.jsxs(hw, {
        children: [e.map(function({id: t, title: n, description: r, action: o, ...i}) {
            return m.jsxs(Om, {
                ...i,
                children: [m.jsxs("div", {
                    className: "grid gap-1",
                    children: [n && m.jsx(Lm, {
                        children: n
                    }), r && m.jsx(Mm, {
                        children: r
                    })]
                }), o, m.jsx(_m, {})]
            }, t)
        }), m.jsx(jm, {})]
    })
}
var Yd = ["light", "dark"]
  , yw = "(prefers-color-scheme: dark)"
  , xw = g.createContext(void 0)
  , ww = {
    setTheme: e => {}
    ,
    themes: []
}
  , Ew = () => {
    var e;
    return (e = g.useContext(xw)) != null ? e : ww
}
;
g.memo( ({forcedTheme: e, storageKey: t, attribute: n, enableSystem: r, enableColorScheme: o, defaultTheme: i, value: s, attrs: l, nonce: a}) => {
    let u = i === "system"
      , d = n === "class" ? `var d=document.documentElement,c=d.classList;${`c.remove(${l.map(w => `'${w}'`).join(",")})`};` : `var d=document.documentElement,n='${n}',s='setAttribute';`
      , f = o ? Yd.includes(i) && i ? `if(e==='light'||e==='dark'||!e)d.style.colorScheme=e||'${i}'` : "if(e==='light'||e==='dark')d.style.colorScheme=e" : ""
      , c = (w, x=!1, E=!0) => {
        let h = s ? s[w] : w
          , p = x ? w + "|| ''" : `'${h}'`
          , v = "";
        return o && E && !x && Yd.includes(w) && (v += `d.style.colorScheme = '${w}';`),
        n === "class" ? x || h ? v += `c.add(${p})` : v += "null" : h && (v += `d[s](n,${p})`),
        v
    }
      , y = e ? `!function(){${d}${c(e)}}()` : r ? `!function(){try{${d}var e=localStorage.getItem('${t}');if('system'===e||(!e&&${u})){var t='${yw}',m=window.matchMedia(t);if(m.media!==t||m.matches){${c("dark")}}else{${c("light")}}}else if(e){${s ? `var x=${JSON.stringify(s)};` : ""}${c(s ? "x[e]" : "e", !0)}}${u ? "" : "else{" + c(i, !1, !1) + "}"}${f}}catch(e){}}()` : `!function(){try{${d}var e=localStorage.getItem('${t}');if(e){${s ? `var x=${JSON.stringify(s)};` : ""}${c(s ? "x[e]" : "e", !0)}}else{${c(i, !1, !1)};}${f}}catch(t){}}();`;
    return g.createElement("script", {
        nonce: a,
        dangerouslySetInnerHTML: {
            __html: y
        }
    })
}
);
var Sw = e => {
    switch (e) {
    case "success":
        return kw;
    case "info":
        return Nw;
    case "warning":
        return Pw;
    case "error":
        return Tw;
    default:
        return null
    }
}
  , Cw = Array(12).fill(0)
  , bw = ({visible: e}) => R.createElement("div", {
    className: "sonner-loading-wrapper",
    "data-visible": e
}, R.createElement("div", {
    className: "sonner-spinner"
}, Cw.map( (t, n) => R.createElement("div", {
    className: "sonner-loading-bar",
    key: `spinner-bar-${n}`
}))))
  , kw = R.createElement("svg", {
    xmlns: "http://www.w3.org/2000/svg",
    viewBox: "0 0 20 20",
    fill: "currentColor",
    height: "20",
    width: "20"
}, R.createElement("path", {
    fillRule: "evenodd",
    d: "M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z",
    clipRule: "evenodd"
}))
  , Pw = R.createElement("svg", {
    xmlns: "http://www.w3.org/2000/svg",
    viewBox: "0 0 24 24",
    fill: "currentColor",
    height: "20",
    width: "20"
}, R.createElement("path", {
    fillRule: "evenodd",
    d: "M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003zM12 8.25a.75.75 0 01.75.75v3.75a.75.75 0 01-1.5 0V9a.75.75 0 01.75-.75zm0 8.25a.75.75 0 100-1.5.75.75 0 000 1.5z",
    clipRule: "evenodd"
}))
  , Nw = R.createElement("svg", {
    xmlns: "http://www.w3.org/2000/svg",
    viewBox: "0 0 20 20",
    fill: "currentColor",
    height: "20",
    width: "20"
}, R.createElement("path", {
    fillRule: "evenodd",
    d: "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z",
    clipRule: "evenodd"
}))
  , Tw = R.createElement("svg", {
    xmlns: "http://www.w3.org/2000/svg",
    viewBox: "0 0 20 20",
    fill: "currentColor",
    height: "20",
    width: "20"
}, R.createElement("path", {
    fillRule: "evenodd",
    d: "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z",
    clipRule: "evenodd"
}))
  , Rw = () => {
    let[e,t] = R.useState(document.hidden);
    return R.useEffect( () => {
        let n = () => {
            t(document.hidden)
        }
        ;
        return document.addEventListener("visibilitychange", n),
        () => window.removeEventListener("visibilitychange", n)
    }
    , []),
    e
}
  , Ya = 1
  , Aw = class {
    constructor() {
        this.subscribe = e => (this.subscribers.push(e),
        () => {
            let t = this.subscribers.indexOf(e);
            this.subscribers.splice(t, 1)
        }
        ),
        this.publish = e => {
            this.subscribers.forEach(t => t(e))
        }
        ,
        this.addToast = e => {
            this.publish(e),
            this.toasts = [...this.toasts, e]
        }
        ,
        this.create = e => {
            var t;
            let {message: n, ...r} = e
              , o = typeof (e == null ? void 0 : e.id) == "number" || ((t = e.id) == null ? void 0 : t.length) > 0 ? e.id : Ya++
              , i = this.toasts.find(l => l.id === o)
              , s = e.dismissible === void 0 ? !0 : e.dismissible;
            return i ? this.toasts = this.toasts.map(l => l.id === o ? (this.publish({
                ...l,
                ...e,
                id: o,
                title: n
            }),
            {
                ...l,
                ...e,
                id: o,
                dismissible: s,
                title: n
            }) : l) : this.addToast({
                title: n,
                ...r,
                dismissible: s,
                id: o
            }),
            o
        }
        ,
        this.dismiss = e => (e || this.toasts.forEach(t => {
            this.subscribers.forEach(n => n({
                id: t.id,
                dismiss: !0
            }))
        }
        ),
        this.subscribers.forEach(t => t({
            id: e,
            dismiss: !0
        })),
        e),
        this.message = (e, t) => this.create({
            ...t,
            message: e
        }),
        this.error = (e, t) => this.create({
            ...t,
            message: e,
            type: "error"
        }),
        this.success = (e, t) => this.create({
            ...t,
            type: "success",
            message: e
        }),
        this.info = (e, t) => this.create({
            ...t,
            type: "info",
            message: e
        }),
        this.warning = (e, t) => this.create({
            ...t,
            type: "warning",
            message: e
        }),
        this.loading = (e, t) => this.create({
            ...t,
            type: "loading",
            message: e
        }),
        this.promise = (e, t) => {
            if (!t)
                return;
            let n;
            t.loading !== void 0 && (n = this.create({
                ...t,
                promise: e,
                type: "loading",
                message: t.loading,
                description: typeof t.description != "function" ? t.description : void 0
            }));
            let r = e instanceof Promise ? e : e()
              , o = n !== void 0;
            return r.then(async i => {
                if (Ow(i) && !i.ok) {
                    o = !1;
                    let s = typeof t.error == "function" ? await t.error(`HTTP error! status: ${i.status}`) : t.error
                      , l = typeof t.description == "function" ? await t.description(`HTTP error! status: ${i.status}`) : t.description;
                    this.create({
                        id: n,
                        type: "error",
                        message: s,
                        description: l
                    })
                } else if (t.success !== void 0) {
                    o = !1;
                    let s = typeof t.success == "function" ? await t.success(i) : t.success
                      , l = typeof t.description == "function" ? await t.description(i) : t.description;
                    this.create({
                        id: n,
                        type: "success",
                        message: s,
                        description: l
                    })
                }
            }
            ).catch(async i => {
                if (t.error !== void 0) {
                    o = !1;
                    let s = typeof t.error == "function" ? await t.error(i) : t.error
                      , l = typeof t.description == "function" ? await t.description(i) : t.description;
                    this.create({
                        id: n,
                        type: "error",
                        message: s,
                        description: l
                    })
                }
            }
            ).finally( () => {
                var i;
                o && (this.dismiss(n),
                n = void 0),
                (i = t.finally) == null || i.call(t)
            }
            ),
            n
        }
        ,
        this.custom = (e, t) => {
            let n = (t == null ? void 0 : t.id) || Ya++;
            return this.create({
                jsx: e(n),
                id: n,
                ...t
            }),
            n
        }
        ,
        this.subscribers = [],
        this.toasts = []
    }
}
  , Qe = new Aw
  , jw = (e, t) => {
    let n = (t == null ? void 0 : t.id) || Ya++;
    return Qe.addToast({
        title: e,
        ...t,
        id: n
    }),
    n
}
  , Ow = e => e && typeof e == "object" && "ok"in e && typeof e.ok == "boolean" && "status"in e && typeof e.status == "number"
  , _w = jw
  , Lw = () => Qe.toasts;
Object.assign(_w, {
    success: Qe.success,
    info: Qe.info,
    warning: Qe.warning,
    error: Qe.error,
    custom: Qe.custom,
    message: Qe.message,
    promise: Qe.promise,
    dismiss: Qe.dismiss,
    loading: Qe.loading
}, {
    getHistory: Lw
});
function Mw(e, {insertAt: t}={}) {
    if (typeof document > "u")
        return;
    let n = document.head || document.getElementsByTagName("head")[0]
      , r = document.createElement("style");
    r.type = "text/css",
    t === "top" && n.firstChild ? n.insertBefore(r, n.firstChild) : n.appendChild(r),
    r.styleSheet ? r.styleSheet.cssText = e : r.appendChild(document.createTextNode(e))
}
Mw(`:where(html[dir="ltr"]),:where([data-sonner-toaster][dir="ltr"]){--toast-icon-margin-start: -3px;--toast-icon-margin-end: 4px;--toast-svg-margin-start: -1px;--toast-svg-margin-end: 0px;--toast-button-margin-start: auto;--toast-button-margin-end: 0;--toast-close-button-start: 0;--toast-close-button-end: unset;--toast-close-button-transform: translate(-35%, -35%)}:where(html[dir="rtl"]),:where([data-sonner-toaster][dir="rtl"]){--toast-icon-margin-start: 4px;--toast-icon-margin-end: -3px;--toast-svg-margin-start: 0px;--toast-svg-margin-end: -1px;--toast-button-margin-start: 0;--toast-button-margin-end: auto;--toast-close-button-start: unset;--toast-close-button-end: 0;--toast-close-button-transform: translate(35%, -35%)}:where([data-sonner-toaster]){position:fixed;width:var(--width);font-family:ui-sans-serif,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica Neue,Arial,Noto Sans,sans-serif,Apple Color Emoji,Segoe UI Emoji,Segoe UI Symbol,Noto Color Emoji;--gray1: hsl(0, 0%, 99%);--gray2: hsl(0, 0%, 97.3%);--gray3: hsl(0, 0%, 95.1%);--gray4: hsl(0, 0%, 93%);--gray5: hsl(0, 0%, 90.9%);--gray6: hsl(0, 0%, 88.7%);--gray7: hsl(0, 0%, 85.8%);--gray8: hsl(0, 0%, 78%);--gray9: hsl(0, 0%, 56.1%);--gray10: hsl(0, 0%, 52.3%);--gray11: hsl(0, 0%, 43.5%);--gray12: hsl(0, 0%, 9%);--border-radius: 8px;box-sizing:border-box;padding:0;margin:0;list-style:none;outline:none;z-index:999999999}:where([data-sonner-toaster][data-x-position="right"]){right:max(var(--offset),env(safe-area-inset-right))}:where([data-sonner-toaster][data-x-position="left"]){left:max(var(--offset),env(safe-area-inset-left))}:where([data-sonner-toaster][data-x-position="center"]){left:50%;transform:translate(-50%)}:where([data-sonner-toaster][data-y-position="top"]){top:max(var(--offset),env(safe-area-inset-top))}:where([data-sonner-toaster][data-y-position="bottom"]){bottom:max(var(--offset),env(safe-area-inset-bottom))}:where([data-sonner-toast]){--y: translateY(100%);--lift-amount: calc(var(--lift) * var(--gap));z-index:var(--z-index);position:absolute;opacity:0;transform:var(--y);filter:blur(0);touch-action:none;transition:transform .4s,opacity .4s,height .4s,box-shadow .2s;box-sizing:border-box;outline:none;overflow-wrap:anywhere}:where([data-sonner-toast][data-styled="true"]){padding:16px;background:var(--normal-bg);border:1px solid var(--normal-border);color:var(--normal-text);border-radius:var(--border-radius);box-shadow:0 4px 12px #0000001a;width:var(--width);font-size:13px;display:flex;align-items:center;gap:6px}:where([data-sonner-toast]:focus-visible){box-shadow:0 4px 12px #0000001a,0 0 0 2px #0003}:where([data-sonner-toast][data-y-position="top"]){top:0;--y: translateY(-100%);--lift: 1;--lift-amount: calc(1 * var(--gap))}:where([data-sonner-toast][data-y-position="bottom"]){bottom:0;--y: translateY(100%);--lift: -1;--lift-amount: calc(var(--lift) * var(--gap))}:where([data-sonner-toast]) :where([data-description]){font-weight:400;line-height:1.4;color:inherit}:where([data-sonner-toast]) :where([data-title]){font-weight:500;line-height:1.5;color:inherit}:where([data-sonner-toast]) :where([data-icon]){display:flex;height:16px;width:16px;position:relative;justify-content:flex-start;align-items:center;flex-shrink:0;margin-left:var(--toast-icon-margin-start);margin-right:var(--toast-icon-margin-end)}:where([data-sonner-toast][data-promise="true"]) :where([data-icon])>svg{opacity:0;transform:scale(.8);transform-origin:center;animation:sonner-fade-in .3s ease forwards}:where([data-sonner-toast]) :where([data-icon])>*{flex-shrink:0}:where([data-sonner-toast]) :where([data-icon]) svg{margin-left:var(--toast-svg-margin-start);margin-right:var(--toast-svg-margin-end)}:where([data-sonner-toast]) :where([data-content]){display:flex;flex-direction:column;gap:2px}[data-sonner-toast][data-styled=true] [data-button]{border-radius:4px;padding-left:8px;padding-right:8px;height:24px;font-size:12px;color:var(--normal-bg);background:var(--normal-text);margin-left:var(--toast-button-margin-start);margin-right:var(--toast-button-margin-end);border:none;cursor:pointer;outline:none;display:flex;align-items:center;flex-shrink:0;transition:opacity .4s,box-shadow .2s}:where([data-sonner-toast]) :where([data-button]):focus-visible{box-shadow:0 0 0 2px #0006}:where([data-sonner-toast]) :where([data-button]):first-of-type{margin-left:var(--toast-button-margin-start);margin-right:var(--toast-button-margin-end)}:where([data-sonner-toast]) :where([data-cancel]){color:var(--normal-text);background:rgba(0,0,0,.08)}:where([data-sonner-toast][data-theme="dark"]) :where([data-cancel]){background:rgba(255,255,255,.3)}:where([data-sonner-toast]) :where([data-close-button]){position:absolute;left:var(--toast-close-button-start);right:var(--toast-close-button-end);top:0;height:20px;width:20px;display:flex;justify-content:center;align-items:center;padding:0;background:var(--gray1);color:var(--gray12);border:1px solid var(--gray4);transform:var(--toast-close-button-transform);border-radius:50%;cursor:pointer;z-index:1;transition:opacity .1s,background .2s,border-color .2s}:where([data-sonner-toast]) :where([data-close-button]):focus-visible{box-shadow:0 4px 12px #0000001a,0 0 0 2px #0003}:where([data-sonner-toast]) :where([data-disabled="true"]){cursor:not-allowed}:where([data-sonner-toast]):hover :where([data-close-button]):hover{background:var(--gray2);border-color:var(--gray5)}:where([data-sonner-toast][data-swiping="true"]):before{content:"";position:absolute;left:0;right:0;height:100%;z-index:-1}:where([data-sonner-toast][data-y-position="top"][data-swiping="true"]):before{bottom:50%;transform:scaleY(3) translateY(50%)}:where([data-sonner-toast][data-y-position="bottom"][data-swiping="true"]):before{top:50%;transform:scaleY(3) translateY(-50%)}:where([data-sonner-toast][data-swiping="false"][data-removed="true"]):before{content:"";position:absolute;inset:0;transform:scaleY(2)}:where([data-sonner-toast]):after{content:"";position:absolute;left:0;height:calc(var(--gap) + 1px);bottom:100%;width:100%}:where([data-sonner-toast][data-mounted="true"]){--y: translateY(0);opacity:1}:where([data-sonner-toast][data-expanded="false"][data-front="false"]){--scale: var(--toasts-before) * .05 + 1;--y: translateY(calc(var(--lift-amount) * var(--toasts-before))) scale(calc(-1 * var(--scale)));height:var(--front-toast-height)}:where([data-sonner-toast])>*{transition:opacity .4s}:where([data-sonner-toast][data-expanded="false"][data-front="false"][data-styled="true"])>*{opacity:0}:where([data-sonner-toast][data-visible="false"]){opacity:0;pointer-events:none}:where([data-sonner-toast][data-mounted="true"][data-expanded="true"]){--y: translateY(calc(var(--lift) * var(--offset)));height:var(--initial-height)}:where([data-sonner-toast][data-removed="true"][data-front="true"][data-swipe-out="false"]){--y: translateY(calc(var(--lift) * -100%));opacity:0}:where([data-sonner-toast][data-removed="true"][data-front="false"][data-swipe-out="false"][data-expanded="true"]){--y: translateY(calc(var(--lift) * var(--offset) + var(--lift) * -100%));opacity:0}:where([data-sonner-toast][data-removed="true"][data-front="false"][data-swipe-out="false"][data-expanded="false"]){--y: translateY(40%);opacity:0;transition:transform .5s,opacity .2s}:where([data-sonner-toast][data-removed="true"][data-front="false"]):before{height:calc(var(--initial-height) + 20%)}[data-sonner-toast][data-swiping=true]{transform:var(--y) translateY(var(--swipe-amount, 0px));transition:none}[data-sonner-toast][data-swipe-out=true][data-y-position=bottom],[data-sonner-toast][data-swipe-out=true][data-y-position=top]{animation:swipe-out .2s ease-out forwards}@keyframes swipe-out{0%{transform:translateY(calc(var(--lift) * var(--offset) + var(--swipe-amount)));opacity:1}to{transform:translateY(calc(var(--lift) * var(--offset) + var(--swipe-amount) + var(--lift) * -100%));opacity:0}}@media (max-width: 600px){[data-sonner-toaster]{position:fixed;--mobile-offset: 16px;right:var(--mobile-offset);left:var(--mobile-offset);width:100%}[data-sonner-toaster] [data-sonner-toast]{left:0;right:0;width:calc(100% - var(--mobile-offset) * 2)}[data-sonner-toaster][data-x-position=left]{left:var(--mobile-offset)}[data-sonner-toaster][data-y-position=bottom]{bottom:20px}[data-sonner-toaster][data-y-position=top]{top:20px}[data-sonner-toaster][data-x-position=center]{left:var(--mobile-offset);right:var(--mobile-offset);transform:none}}[data-sonner-toaster][data-theme=light]{--normal-bg: #fff;--normal-border: var(--gray4);--normal-text: var(--gray12);--success-bg: hsl(143, 85%, 96%);--success-border: hsl(145, 92%, 91%);--success-text: hsl(140, 100%, 27%);--info-bg: hsl(208, 100%, 97%);--info-border: hsl(221, 91%, 91%);--info-text: hsl(210, 92%, 45%);--warning-bg: hsl(49, 100%, 97%);--warning-border: hsl(49, 91%, 91%);--warning-text: hsl(31, 92%, 45%);--error-bg: hsl(359, 100%, 97%);--error-border: hsl(359, 100%, 94%);--error-text: hsl(360, 100%, 45%)}[data-sonner-toaster][data-theme=light] [data-sonner-toast][data-invert=true]{--normal-bg: #000;--normal-border: hsl(0, 0%, 20%);--normal-text: var(--gray1)}[data-sonner-toaster][data-theme=dark] [data-sonner-toast][data-invert=true]{--normal-bg: #fff;--normal-border: var(--gray3);--normal-text: var(--gray12)}[data-sonner-toaster][data-theme=dark]{--normal-bg: #000;--normal-border: hsl(0, 0%, 20%);--normal-text: var(--gray1);--success-bg: hsl(150, 100%, 6%);--success-border: hsl(147, 100%, 12%);--success-text: hsl(150, 86%, 65%);--info-bg: hsl(215, 100%, 6%);--info-border: hsl(223, 100%, 12%);--info-text: hsl(216, 87%, 65%);--warning-bg: hsl(64, 100%, 6%);--warning-border: hsl(60, 100%, 12%);--warning-text: hsl(46, 87%, 65%);--error-bg: hsl(358, 76%, 10%);--error-border: hsl(357, 89%, 16%);--error-text: hsl(358, 100%, 81%)}[data-rich-colors=true][data-sonner-toast][data-type=success],[data-rich-colors=true][data-sonner-toast][data-type=success] [data-close-button]{background:var(--success-bg);border-color:var(--success-border);color:var(--success-text)}[data-rich-colors=true][data-sonner-toast][data-type=info],[data-rich-colors=true][data-sonner-toast][data-type=info] [data-close-button]{background:var(--info-bg);border-color:var(--info-border);color:var(--info-text)}[data-rich-colors=true][data-sonner-toast][data-type=warning],[data-rich-colors=true][data-sonner-toast][data-type=warning] [data-close-button]{background:var(--warning-bg);border-color:var(--warning-border);color:var(--warning-text)}[data-rich-colors=true][data-sonner-toast][data-type=error],[data-rich-colors=true][data-sonner-toast][data-type=error] [data-close-button]{background:var(--error-bg);border-color:var(--error-border);color:var(--error-text)}.sonner-loading-wrapper{--size: 16px;height:var(--size);width:var(--size);position:absolute;inset:0;z-index:10}.sonner-loading-wrapper[data-visible=false]{transform-origin:center;animation:sonner-fade-out .2s ease forwards}.sonner-spinner{position:relative;top:50%;left:50%;height:var(--size);width:var(--size)}.sonner-loading-bar{animation:sonner-spin 1.2s linear infinite;background:var(--gray11);border-radius:6px;height:8%;left:-10%;position:absolute;top:-3.9%;width:24%}.sonner-loading-bar:nth-child(1){animation-delay:-1.2s;transform:rotate(.0001deg) translate(146%)}.sonner-loading-bar:nth-child(2){animation-delay:-1.1s;transform:rotate(30deg) translate(146%)}.sonner-loading-bar:nth-child(3){animation-delay:-1s;transform:rotate(60deg) translate(146%)}.sonner-loading-bar:nth-child(4){animation-delay:-.9s;transform:rotate(90deg) translate(146%)}.sonner-loading-bar:nth-child(5){animation-delay:-.8s;transform:rotate(120deg) translate(146%)}.sonner-loading-bar:nth-child(6){animation-delay:-.7s;transform:rotate(150deg) translate(146%)}.sonner-loading-bar:nth-child(7){animation-delay:-.6s;transform:rotate(180deg) translate(146%)}.sonner-loading-bar:nth-child(8){animation-delay:-.5s;transform:rotate(210deg) translate(146%)}.sonner-loading-bar:nth-child(9){animation-delay:-.4s;transform:rotate(240deg) translate(146%)}.sonner-loading-bar:nth-child(10){animation-delay:-.3s;transform:rotate(270deg) translate(146%)}.sonner-loading-bar:nth-child(11){animation-delay:-.2s;transform:rotate(300deg) translate(146%)}.sonner-loading-bar:nth-child(12){animation-delay:-.1s;transform:rotate(330deg) translate(146%)}@keyframes sonner-fade-in{0%{opacity:0;transform:scale(.8)}to{opacity:1;transform:scale(1)}}@keyframes sonner-fade-out{0%{opacity:1;transform:scale(1)}to{opacity:0;transform:scale(.8)}}@keyframes sonner-spin{0%{opacity:1}to{opacity:.15}}@media (prefers-reduced-motion){[data-sonner-toast],[data-sonner-toast]>*,.sonner-loading-bar{transition:none!important;animation:none!important}}.sonner-loader{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);transform-origin:center;transition:opacity .2s,transform .2s}.sonner-loader[data-visible=false]{opacity:0;transform:scale(.8) translate(-50%,-50%)}
`);
function zi(e) {
    return e.label !== void 0
}
var Iw = 3
  , Dw = "32px"
  , zw = 4e3
  , Fw = 356
  , $w = 14
  , Uw = 20
  , Bw = 200;
function Ww(...e) {
    return e.filter(Boolean).join(" ")
}
var Vw = e => {
    var t, n, r, o, i, s, l, a, u, d;
    let {invert: f, toast: c, unstyled: y, interacting: w, setHeights: x, visibleToasts: E, heights: h, index: p, toasts: v, expanded: S, removeToast: C, defaultRichColors: P, closeButton: b, style: N, cancelButtonStyle: _, actionButtonStyle: O, className: $="", descriptionClassName: D="", duration: H, position: L, gap: Q, loadingIcon: U, expandByDefault: K, classNames: k, icons: j, closeButtonAriaLabel: z="Close toast", pauseWhenPageIsHidden: I, cn: F} = e
      , [Y,ae] = R.useState(!1)
      , [Ve,Z] = R.useState(!1)
      , [ut,qt] = R.useState(!1)
      , [Zt,Jt] = R.useState(!1)
      , [hi,lr] = R.useState(0)
      , [Dn,so] = R.useState(0)
      , mi = R.useRef(null)
      , en = R.useRef(null)
      , fl = p === 0
      , pl = p + 1 <= E
      , Se = c.type
      , ar = c.dismissible !== !1
      , eg = c.className || ""
      , tg = c.descriptionClassName || ""
      , vi = R.useMemo( () => h.findIndex(B => B.toastId === c.id) || 0, [h, c.id])
      , ng = R.useMemo( () => {
        var B;
        return (B = c.closeButton) != null ? B : b
    }
    , [c.closeButton, b])
      , Pc = R.useMemo( () => c.duration || H || zw, [c.duration, H])
      , hl = R.useRef(0)
      , ur = R.useRef(0)
      , Nc = R.useRef(0)
      , cr = R.useRef(null)
      , [Tc,rg] = L.split("-")
      , Rc = R.useMemo( () => h.reduce( (B, ne, ee) => ee >= vi ? B : B + ne.height, 0), [h, vi])
      , Ac = Rw()
      , og = c.invert || f
      , ml = Se === "loading";
    ur.current = R.useMemo( () => vi * Q + Rc, [vi, Rc]),
    R.useEffect( () => {
        ae(!0)
    }
    , []),
    R.useLayoutEffect( () => {
        if (!Y)
            return;
        let B = en.current
          , ne = B.style.height;
        B.style.height = "auto";
        let ee = B.getBoundingClientRect().height;
        B.style.height = ne,
        so(ee),
        x(bt => bt.find(kt => kt.toastId === c.id) ? bt.map(kt => kt.toastId === c.id ? {
            ...kt,
            height: ee
        } : kt) : [{
            toastId: c.id,
            height: ee,
            position: c.position
        }, ...bt])
    }
    , [Y, c.title, c.description, x, c.id]);
    let tn = R.useCallback( () => {
        Z(!0),
        lr(ur.current),
        x(B => B.filter(ne => ne.toastId !== c.id)),
        setTimeout( () => {
            C(c)
        }
        , Bw)
    }
    , [c, C, x, ur]);
    R.useEffect( () => {
        if (c.promise && Se === "loading" || c.duration === 1 / 0 || c.type === "loading")
            return;
        let B, ne = Pc;
        return S || w || I && Ac ? ( () => {
            if (Nc.current < hl.current) {
                let ee = new Date().getTime() - hl.current;
                ne = ne - ee
            }
            Nc.current = new Date().getTime()
        }
        )() : ne !== 1 / 0 && (hl.current = new Date().getTime(),
        B = setTimeout( () => {
            var ee;
            (ee = c.onAutoClose) == null || ee.call(c, c),
            tn()
        }
        , ne)),
        () => clearTimeout(B)
    }
    , [S, w, K, c, Pc, tn, c.promise, Se, I, Ac]),
    R.useEffect( () => {
        let B = en.current;
        if (B) {
            let ne = B.getBoundingClientRect().height;
            return so(ne),
            x(ee => [{
                toastId: c.id,
                height: ne,
                position: c.position
            }, ...ee]),
            () => x(ee => ee.filter(bt => bt.toastId !== c.id))
        }
    }
    , [x, c.id]),
    R.useEffect( () => {
        c.delete && tn()
    }
    , [tn, c.delete]);
    function ig() {
        return j != null && j.loading ? R.createElement("div", {
            className: "sonner-loader",
            "data-visible": Se === "loading"
        }, j.loading) : U ? R.createElement("div", {
            className: "sonner-loader",
            "data-visible": Se === "loading"
        }, U) : R.createElement(bw, {
            visible: Se === "loading"
        })
    }
    return R.createElement("li", {
        "aria-live": c.important ? "assertive" : "polite",
        "aria-atomic": "true",
        role: "status",
        tabIndex: 0,
        ref: en,
        className: F($, eg, k == null ? void 0 : k.toast, (t = c == null ? void 0 : c.classNames) == null ? void 0 : t.toast, k == null ? void 0 : k.default, k == null ? void 0 : k[Se], (n = c == null ? void 0 : c.classNames) == null ? void 0 : n[Se]),
        "data-sonner-toast": "",
        "data-rich-colors": (r = c.richColors) != null ? r : P,
        "data-styled": !(c.jsx || c.unstyled || y),
        "data-mounted": Y,
        "data-promise": !!c.promise,
        "data-removed": Ve,
        "data-visible": pl,
        "data-y-position": Tc,
        "data-x-position": rg,
        "data-index": p,
        "data-front": fl,
        "data-swiping": ut,
        "data-dismissible": ar,
        "data-type": Se,
        "data-invert": og,
        "data-swipe-out": Zt,
        "data-expanded": !!(S || K && Y),
        style: {
            "--index": p,
            "--toasts-before": p,
            "--z-index": v.length - p,
            "--offset": `${Ve ? hi : ur.current}px`,
            "--initial-height": K ? "auto" : `${Dn}px`,
            ...N,
            ...c.style
        },
        onPointerDown: B => {
            ml || !ar || (mi.current = new Date,
            lr(ur.current),
            B.target.setPointerCapture(B.pointerId),
            B.target.tagName !== "BUTTON" && (qt(!0),
            cr.current = {
                x: B.clientX,
                y: B.clientY
            }))
        }
        ,
        onPointerUp: () => {
            var B, ne, ee, bt;
            if (Zt || !ar)
                return;
            cr.current = null;
            let kt = Number(((B = en.current) == null ? void 0 : B.style.getPropertyValue("--swipe-amount").replace("px", "")) || 0)
              , gi = new Date().getTime() - ((ne = mi.current) == null ? void 0 : ne.getTime())
              , sg = Math.abs(kt) / gi;
            if (Math.abs(kt) >= Uw || sg > .11) {
                lr(ur.current),
                (ee = c.onDismiss) == null || ee.call(c, c),
                tn(),
                Jt(!0);
                return
            }
            (bt = en.current) == null || bt.style.setProperty("--swipe-amount", "0px"),
            qt(!1)
        }
        ,
        onPointerMove: B => {
            var ne;
            if (!cr.current || !ar)
                return;
            let ee = B.clientY - cr.current.y
              , bt = B.clientX - cr.current.x
              , kt = (Tc === "top" ? Math.min : Math.max)(0, ee)
              , gi = B.pointerType === "touch" ? 10 : 2;
            Math.abs(kt) > gi ? (ne = en.current) == null || ne.style.setProperty("--swipe-amount", `${ee}px`) : Math.abs(bt) > gi && (cr.current = null)
        }
    }, ng && !c.jsx ? R.createElement("button", {
        "aria-label": z,
        "data-disabled": ml,
        "data-close-button": !0,
        onClick: ml || !ar ? () => {}
        : () => {
            var B;
            tn(),
            (B = c.onDismiss) == null || B.call(c, c)
        }
        ,
        className: F(k == null ? void 0 : k.closeButton, (o = c == null ? void 0 : c.classNames) == null ? void 0 : o.closeButton)
    }, R.createElement("svg", {
        xmlns: "http://www.w3.org/2000/svg",
        width: "12",
        height: "12",
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        strokeWidth: "1.5",
        strokeLinecap: "round",
        strokeLinejoin: "round"
    }, R.createElement("line", {
        x1: "18",
        y1: "6",
        x2: "6",
        y2: "18"
    }), R.createElement("line", {
        x1: "6",
        y1: "6",
        x2: "18",
        y2: "18"
    }))) : null, c.jsx || R.isValidElement(c.title) ? c.jsx || c.title : R.createElement(R.Fragment, null, Se || c.icon || c.promise ? R.createElement("div", {
        "data-icon": "",
        className: F(k == null ? void 0 : k.icon, (i = c == null ? void 0 : c.classNames) == null ? void 0 : i.icon)
    }, c.promise || c.type === "loading" && !c.icon ? c.icon || ig() : null, c.type !== "loading" ? c.icon || (j == null ? void 0 : j[Se]) || Sw(Se) : null) : null, R.createElement("div", {
        "data-content": "",
        className: F(k == null ? void 0 : k.content, (s = c == null ? void 0 : c.classNames) == null ? void 0 : s.content)
    }, R.createElement("div", {
        "data-title": "",
        className: F(k == null ? void 0 : k.title, (l = c == null ? void 0 : c.classNames) == null ? void 0 : l.title)
    }, c.title), c.description ? R.createElement("div", {
        "data-description": "",
        className: F(D, tg, k == null ? void 0 : k.description, (a = c == null ? void 0 : c.classNames) == null ? void 0 : a.description)
    }, c.description) : null), R.isValidElement(c.cancel) ? c.cancel : c.cancel && zi(c.cancel) ? R.createElement("button", {
        "data-button": !0,
        "data-cancel": !0,
        style: c.cancelButtonStyle || _,
        onClick: B => {
            var ne, ee;
            zi(c.cancel) && ar && ((ee = (ne = c.cancel).onClick) == null || ee.call(ne, B),
            tn())
        }
        ,
        className: F(k == null ? void 0 : k.cancelButton, (u = c == null ? void 0 : c.classNames) == null ? void 0 : u.cancelButton)
    }, c.cancel.label) : null, R.isValidElement(c.action) ? c.action : c.action && zi(c.action) ? R.createElement("button", {
        "data-button": !0,
        "data-action": !0,
        style: c.actionButtonStyle || O,
        onClick: B => {
            var ne, ee;
            zi(c.action) && (B.defaultPrevented || ((ee = (ne = c.action).onClick) == null || ee.call(ne, B),
            tn()))
        }
        ,
        className: F(k == null ? void 0 : k.actionButton, (d = c == null ? void 0 : c.classNames) == null ? void 0 : d.actionButton)
    }, c.action.label) : null))
}
;
function Xd() {
    if (typeof window > "u" || typeof document > "u")
        return "ltr";
    let e = document.documentElement.getAttribute("dir");
    return e === "auto" || !e ? window.getComputedStyle(document.documentElement).direction : e
}
var Hw = e => {
    let {invert: t, position: n="bottom-right", hotkey: r=["altKey", "KeyT"], expand: o, closeButton: i, className: s, offset: l, theme: a="light", richColors: u, duration: d, style: f, visibleToasts: c=Iw, toastOptions: y, dir: w=Xd(), gap: x=$w, loadingIcon: E, icons: h, containerAriaLabel: p="Notifications", pauseWhenPageIsHidden: v, cn: S=Ww} = e
      , [C,P] = R.useState([])
      , b = R.useMemo( () => Array.from(new Set([n].concat(C.filter(I => I.position).map(I => I.position)))), [C, n])
      , [N,_] = R.useState([])
      , [O,$] = R.useState(!1)
      , [D,H] = R.useState(!1)
      , [L,Q] = R.useState(a !== "system" ? a : typeof window < "u" && window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
      , U = R.useRef(null)
      , K = r.join("+").replace(/Key/g, "").replace(/Digit/g, "")
      , k = R.useRef(null)
      , j = R.useRef(!1)
      , z = R.useCallback(I => {
        var F;
        (F = C.find(Y => Y.id === I.id)) != null && F.delete || Qe.dismiss(I.id),
        P(Y => Y.filter( ({id: ae}) => ae !== I.id))
    }
    , [C]);
    return R.useEffect( () => Qe.subscribe(I => {
        if (I.dismiss) {
            P(F => F.map(Y => Y.id === I.id ? {
                ...Y,
                delete: !0
            } : Y));
            return
        }
        setTimeout( () => {
            Qh.flushSync( () => {
                P(F => {
                    let Y = F.findIndex(ae => ae.id === I.id);
                    return Y !== -1 ? [...F.slice(0, Y), {
                        ...F[Y],
                        ...I
                    }, ...F.slice(Y + 1)] : [I, ...F]
                }
                )
            }
            )
        }
        )
    }
    ), []),
    R.useEffect( () => {
        if (a !== "system") {
            Q(a);
            return
        }
        a === "system" && (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? Q("dark") : Q("light")),
        typeof window < "u" && window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", ({matches: I}) => {
            Q(I ? "dark" : "light")
        }
        )
    }
    , [a]),
    R.useEffect( () => {
        C.length <= 1 && $(!1)
    }
    , [C]),
    R.useEffect( () => {
        let I = F => {
            var Y, ae;
            r.every(Ve => F[Ve] || F.code === Ve) && ($(!0),
            (Y = U.current) == null || Y.focus()),
            F.code === "Escape" && (document.activeElement === U.current || (ae = U.current) != null && ae.contains(document.activeElement)) && $(!1)
        }
        ;
        return document.addEventListener("keydown", I),
        () => document.removeEventListener("keydown", I)
    }
    , [r]),
    R.useEffect( () => {
        if (U.current)
            return () => {
                k.current && (k.current.focus({
                    preventScroll: !0
                }),
                k.current = null,
                j.current = !1)
            }
    }
    , [U.current]),
    C.length ? R.createElement("section", {
        "aria-label": `${p} ${K}`,
        tabIndex: -1
    }, b.map( (I, F) => {
        var Y;
        let[ae,Ve] = I.split("-");
        return R.createElement("ol", {
            key: I,
            dir: w === "auto" ? Xd() : w,
            tabIndex: -1,
            ref: U,
            className: s,
            "data-sonner-toaster": !0,
            "data-theme": L,
            "data-y-position": ae,
            "data-x-position": Ve,
            style: {
                "--front-toast-height": `${((Y = N[0]) == null ? void 0 : Y.height) || 0}px`,
                "--offset": typeof l == "number" ? `${l}px` : l || Dw,
                "--width": `${Fw}px`,
                "--gap": `${x}px`,
                ...f
            },
            onBlur: Z => {
                j.current && !Z.currentTarget.contains(Z.relatedTarget) && (j.current = !1,
                k.current && (k.current.focus({
                    preventScroll: !0
                }),
                k.current = null))
            }
            ,
            onFocus: Z => {
                Z.target instanceof HTMLElement && Z.target.dataset.dismissible === "false" || j.current || (j.current = !0,
                k.current = Z.relatedTarget)
            }
            ,
            onMouseEnter: () => $(!0),
            onMouseMove: () => $(!0),
            onMouseLeave: () => {
                D || $(!1)
            }
            ,
            onPointerDown: Z => {
                Z.target instanceof HTMLElement && Z.target.dataset.dismissible === "false" || H(!0)
            }
            ,
            onPointerUp: () => H(!1)
        }, C.filter(Z => !Z.position && F === 0 || Z.position === I).map( (Z, ut) => {
            var qt, Zt;
            return R.createElement(Vw, {
                key: Z.id,
                icons: h,
                index: ut,
                toast: Z,
                defaultRichColors: u,
                duration: (qt = y == null ? void 0 : y.duration) != null ? qt : d,
                className: y == null ? void 0 : y.className,
                descriptionClassName: y == null ? void 0 : y.descriptionClassName,
                invert: t,
                visibleToasts: c,
                closeButton: (Zt = y == null ? void 0 : y.closeButton) != null ? Zt : i,
                interacting: D,
                position: I,
                style: y == null ? void 0 : y.style,
                unstyled: y == null ? void 0 : y.unstyled,
                classNames: y == null ? void 0 : y.classNames,
                cancelButtonStyle: y == null ? void 0 : y.cancelButtonStyle,
                actionButtonStyle: y == null ? void 0 : y.actionButtonStyle,
                removeToast: z,
                toasts: C.filter(Jt => Jt.position == Z.position),
                heights: N.filter(Jt => Jt.position == Z.position),
                setHeights: _,
                expandByDefault: o,
                gap: x,
                loadingIcon: E,
                expanded: O,
                pauseWhenPageIsHidden: v,
                cn: S
            })
        }
        ))
    }
    )) : null
}
;
const Qw = ({...e}) => {
    const {theme: t="system"} = Ew();
    return m.jsx(Hw, {
        theme: t,
        className: "toaster group",
        toastOptions: {
            classNames: {
                toast: "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
                description: "group-[.toast]:text-muted-foreground",
                actionButton: "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
                cancelButton: "group-[.toast]:bg-muted group-[.toast]:text-muted-foreground"
            }
        },
        ...e
    })
}
;
function Kw(e, t=[]) {
    let n = [];
    function r(i, s) {
        const l = g.createContext(s)
          , a = n.length;
        n = [...n, s];
        const u = f => {
            var h;
            const {scope: c, children: y, ...w} = f
              , x = ((h = c == null ? void 0 : c[e]) == null ? void 0 : h[a]) || l
              , E = g.useMemo( () => w, Object.values(w));
            return m.jsx(x.Provider, {
                value: E,
                children: y
            })
        }
        ;
        u.displayName = i + "Provider";
        function d(f, c) {
            var x;
            const y = ((x = c == null ? void 0 : c[e]) == null ? void 0 : x[a]) || l
              , w = g.useContext(y);
            if (w)
                return w;
            if (s !== void 0)
                return s;
            throw new Error(`\`${f}\` must be used within \`${i}\``)
        }
        return [u, d]
    }
    const o = () => {
        const i = n.map(s => g.createContext(s));
        return function(l) {
            const a = (l == null ? void 0 : l[e]) || i;
            return g.useMemo( () => ({
                [`__scope${e}`]: {
                    ...l,
                    [e]: a
                }
            }), [l, a])
        }
    }
    ;
    return o.scopeName = e,
    [r, Gw(o, ...t)]
}
function Gw(...e) {
    const t = e[0];
    if (e.length === 1)
        return t;
    const n = () => {
        const r = e.map(o => ({
            useScope: o(),
            scopeName: o.scopeName
        }));
        return function(i) {
            const s = r.reduce( (l, {useScope: a, scopeName: u}) => {
                const f = a(i)[`__scope${u}`];
                return {
                    ...l,
                    ...f
                }
            }
            , {});
            return g.useMemo( () => ({
                [`__scope${t.scopeName}`]: s
            }), [s])
        }
    }
    ;
    return n.scopeName = t.scopeName,
    n
}
var Yw = "DismissableLayer", Xa = "dismissableLayer.update", Xw = "dismissableLayer.pointerDownOutside", qw = "dismissableLayer.focusOutside", qd, Im = g.createContext({
    layers: new Set,
    layersWithOutsidePointerEventsDisabled: new Set,
    branches: new Set
}), Dm = g.forwardRef( (e, t) => {
    const {disableOutsidePointerEvents: n=!1, onEscapeKeyDown: r, onPointerDownOutside: o, onFocusOutside: i, onInteractOutside: s, onDismiss: l, ...a} = e
      , u = g.useContext(Im)
      , [d,f] = g.useState(null)
      , c = (d == null ? void 0 : d.ownerDocument) ?? (globalThis == null ? void 0 : globalThis.document)
      , [,y] = g.useState({})
      , w = Oe(t, b => f(b))
      , x = Array.from(u.layers)
      , [E] = [...u.layersWithOutsidePointerEventsDisabled].slice(-1)
      , h = x.indexOf(E)
      , p = d ? x.indexOf(d) : -1
      , v = u.layersWithOutsidePointerEventsDisabled.size > 0
      , S = p >= h
      , C = e1(b => {
        const N = b.target
          , _ = [...u.branches].some(O => O.contains(N));
        !S || _ || (o == null || o(b),
        s == null || s(b),
        b.defaultPrevented || l == null || l())
    }
    , c)
      , P = t1(b => {
        const N = b.target;
        [...u.branches].some(O => O.contains(N)) || (i == null || i(b),
        s == null || s(b),
        b.defaultPrevented || l == null || l())
    }
    , c);
    return qh(b => {
        p === u.layers.size - 1 && (r == null || r(b),
        !b.defaultPrevented && l && (b.preventDefault(),
        l()))
    }
    , c),
    g.useEffect( () => {
        if (d)
            return n && (u.layersWithOutsidePointerEventsDisabled.size === 0 && (qd = c.body.style.pointerEvents,
            c.body.style.pointerEvents = "none"),
            u.layersWithOutsidePointerEventsDisabled.add(d)),
            u.layers.add(d),
            Zd(),
            () => {
                n && u.layersWithOutsidePointerEventsDisabled.size === 1 && (c.body.style.pointerEvents = qd)
            }
    }
    , [d, c, n, u]),
    g.useEffect( () => () => {
        d && (u.layers.delete(d),
        u.layersWithOutsidePointerEventsDisabled.delete(d),
        Zd())
    }
    , [d, u]),
    g.useEffect( () => {
        const b = () => y({});
        return document.addEventListener(Xa, b),
        () => document.removeEventListener(Xa, b)
    }
    , []),
    m.jsx(me.div, {
        ...a,
        ref: w,
        style: {
            pointerEvents: v ? S ? "auto" : "none" : void 0,
            ...e.style
        },
        onFocusCapture: le(e.onFocusCapture, P.onFocusCapture),
        onBlurCapture: le(e.onBlurCapture, P.onBlurCapture),
        onPointerDownCapture: le(e.onPointerDownCapture, C.onPointerDownCapture)
    })
}
);
Dm.displayName = Yw;
var Zw = "DismissableLayerBranch"
  , Jw = g.forwardRef( (e, t) => {
    const n = g.useContext(Im)
      , r = g.useRef(null)
      , o = Oe(t, r);
    return g.useEffect( () => {
        const i = r.current;
        if (i)
            return n.branches.add(i),
            () => {
                n.branches.delete(i)
            }
    }
    , [n.branches]),
    m.jsx(me.div, {
        ...e,
        ref: o
    })
}
);
Jw.displayName = Zw;
function e1(e, t=globalThis == null ? void 0 : globalThis.document) {
    const n = at(e)
      , r = g.useRef(!1)
      , o = g.useRef( () => {}
    );
    return g.useEffect( () => {
        const i = l => {
            if (l.target && !r.current) {
                let a = function() {
                    zm(Xw, n, u, {
                        discrete: !0
                    })
                };
                const u = {
                    originalEvent: l
                };
                l.pointerType === "touch" ? (t.removeEventListener("click", o.current),
                o.current = a,
                t.addEventListener("click", o.current, {
                    once: !0
                })) : a()
            } else
                t.removeEventListener("click", o.current);
            r.current = !1
        }
          , s = window.setTimeout( () => {
            t.addEventListener("pointerdown", i)
        }
        , 0);
        return () => {
            window.clearTimeout(s),
            t.removeEventListener("pointerdown", i),
            t.removeEventListener("click", o.current)
        }
    }
    , [t, n]),
    {
        onPointerDownCapture: () => r.current = !0
    }
}
function t1(e, t=globalThis == null ? void 0 : globalThis.document) {
    const n = at(e)
      , r = g.useRef(!1);
    return g.useEffect( () => {
        const o = i => {
            i.target && !r.current && zm(qw, n, {
                originalEvent: i
            }, {
                discrete: !1
            })
        }
        ;
        return t.addEventListener("focusin", o),
        () => t.removeEventListener("focusin", o)
    }
    , [t, n]),
    {
        onFocusCapture: () => r.current = !0,
        onBlurCapture: () => r.current = !1
    }
}
function Zd() {
    const e = new CustomEvent(Xa);
    document.dispatchEvent(e)
}
function zm(e, t, n, {discrete: r}) {
    const o = n.originalEvent.target
      , i = new CustomEvent(e,{
        bubbles: !1,
        cancelable: !0,
        detail: n
    });
    t && o.addEventListener(e, t, {
        once: !0
    }),
    r ? tc(o, i) : o.dispatchEvent(i)
}
var n1 = Wf.useId || ( () => {}
)
  , r1 = 0;
function Fm(e) {
    const [t,n] = g.useState(n1());
    return xt( () => {
        n(r => r ?? String(r1++))
    }
    , [e]),
    t ? `radix-${t}` : ""
}
const o1 = ["top", "right", "bottom", "left"]
  , An = Math.min
  , Ge = Math.max
  , Ns = Math.round
  , Fi = Math.floor
  , jn = e => ({
    x: e,
    y: e
})
  , i1 = {
    left: "right",
    right: "left",
    bottom: "top",
    top: "bottom"
}
  , s1 = {
    start: "end",
    end: "start"
};
function qa(e, t, n) {
    return Ge(e, An(t, n))
}
function Kt(e, t) {
    return typeof e == "function" ? e(t) : e
}
function Gt(e) {
    return e.split("-")[0]
}
function to(e) {
    return e.split("-")[1]
}
function ac(e) {
    return e === "x" ? "y" : "x"
}
function uc(e) {
    return e === "y" ? "height" : "width"
}
function On(e) {
    return ["top", "bottom"].includes(Gt(e)) ? "y" : "x"
}
function cc(e) {
    return ac(On(e))
}
function l1(e, t, n) {
    n === void 0 && (n = !1);
    const r = to(e)
      , o = cc(e)
      , i = uc(o);
    let s = o === "x" ? r === (n ? "end" : "start") ? "right" : "left" : r === "start" ? "bottom" : "top";
    return t.reference[i] > t.floating[i] && (s = Ts(s)),
    [s, Ts(s)]
}
function a1(e) {
    const t = Ts(e);
    return [Za(e), t, Za(t)]
}
function Za(e) {
    return e.replace(/start|end/g, t => s1[t])
}
function u1(e, t, n) {
    const r = ["left", "right"]
      , o = ["right", "left"]
      , i = ["top", "bottom"]
      , s = ["bottom", "top"];
    switch (e) {
    case "top":
    case "bottom":
        return n ? t ? o : r : t ? r : o;
    case "left":
    case "right":
        return t ? i : s;
    default:
        return []
    }
}
function c1(e, t, n, r) {
    const o = to(e);
    let i = u1(Gt(e), n === "start", r);
    return o && (i = i.map(s => s + "-" + o),
    t && (i = i.concat(i.map(Za)))),
    i
}
function Ts(e) {
    return e.replace(/left|right|bottom|top/g, t => i1[t])
}
function d1(e) {
    return {
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        ...e
    }
}
function $m(e) {
    return typeof e != "number" ? d1(e) : {
        top: e,
        right: e,
        bottom: e,
        left: e
    }
}
function Rs(e) {
    const {x: t, y: n, width: r, height: o} = e;
    return {
        width: r,
        height: o,
        top: n,
        left: t,
        right: t + r,
        bottom: n + o,
        x: t,
        y: n
    }
}
function Jd(e, t, n) {
    let {reference: r, floating: o} = e;
    const i = On(t)
      , s = cc(t)
      , l = uc(s)
      , a = Gt(t)
      , u = i === "y"
      , d = r.x + r.width / 2 - o.width / 2
      , f = r.y + r.height / 2 - o.height / 2
      , c = r[l] / 2 - o[l] / 2;
    let y;
    switch (a) {
    case "top":
        y = {
            x: d,
            y: r.y - o.height
        };
        break;
    case "bottom":
        y = {
            x: d,
            y: r.y + r.height
        };
        break;
    case "right":
        y = {
            x: r.x + r.width,
            y: f
        };
        break;
    case "left":
        y = {
            x: r.x - o.width,
            y: f
        };
        break;
    default:
        y = {
            x: r.x,
            y: r.y
        }
    }
    switch (to(t)) {
    case "start":
        y[s] -= c * (n && u ? -1 : 1);
        break;
    case "end":
        y[s] += c * (n && u ? -1 : 1);
        break
    }
    return y
}
const f1 = async (e, t, n) => {
    const {placement: r="bottom", strategy: o="absolute", middleware: i=[], platform: s} = n
      , l = i.filter(Boolean)
      , a = await (s.isRTL == null ? void 0 : s.isRTL(t));
    let u = await s.getElementRects({
        reference: e,
        floating: t,
        strategy: o
    })
      , {x: d, y: f} = Jd(u, r, a)
      , c = r
      , y = {}
      , w = 0;
    for (let x = 0; x < l.length; x++) {
        const {name: E, fn: h} = l[x]
          , {x: p, y: v, data: S, reset: C} = await h({
            x: d,
            y: f,
            initialPlacement: r,
            placement: c,
            strategy: o,
            middlewareData: y,
            rects: u,
            platform: s,
            elements: {
                reference: e,
                floating: t
            }
        });
        d = p ?? d,
        f = v ?? f,
        y = {
            ...y,
            [E]: {
                ...y[E],
                ...S
            }
        },
        C && w <= 50 && (w++,
        typeof C == "object" && (C.placement && (c = C.placement),
        C.rects && (u = C.rects === !0 ? await s.getElementRects({
            reference: e,
            floating: t,
            strategy: o
        }) : C.rects),
        {x: d, y: f} = Jd(u, c, a)),
        x = -1)
    }
    return {
        x: d,
        y: f,
        placement: c,
        strategy: o,
        middlewareData: y
    }
}
;
async function qo(e, t) {
    var n;
    t === void 0 && (t = {});
    const {x: r, y: o, platform: i, rects: s, elements: l, strategy: a} = e
      , {boundary: u="clippingAncestors", rootBoundary: d="viewport", elementContext: f="floating", altBoundary: c=!1, padding: y=0} = Kt(t, e)
      , w = $m(y)
      , E = l[c ? f === "floating" ? "reference" : "floating" : f]
      , h = Rs(await i.getClippingRect({
        element: (n = await (i.isElement == null ? void 0 : i.isElement(E))) == null || n ? E : E.contextElement || await (i.getDocumentElement == null ? void 0 : i.getDocumentElement(l.floating)),
        boundary: u,
        rootBoundary: d,
        strategy: a
    }))
      , p = f === "floating" ? {
        x: r,
        y: o,
        width: s.floating.width,
        height: s.floating.height
    } : s.reference
      , v = await (i.getOffsetParent == null ? void 0 : i.getOffsetParent(l.floating))
      , S = await (i.isElement == null ? void 0 : i.isElement(v)) ? await (i.getScale == null ? void 0 : i.getScale(v)) || {
        x: 1,
        y: 1
    } : {
        x: 1,
        y: 1
    }
      , C = Rs(i.convertOffsetParentRelativeRectToViewportRelativeRect ? await i.convertOffsetParentRelativeRectToViewportRelativeRect({
        elements: l,
        rect: p,
        offsetParent: v,
        strategy: a
    }) : p);
    return {
        top: (h.top - C.top + w.top) / S.y,
        bottom: (C.bottom - h.bottom + w.bottom) / S.y,
        left: (h.left - C.left + w.left) / S.x,
        right: (C.right - h.right + w.right) / S.x
    }
}
const p1 = e => ({
    name: "arrow",
    options: e,
    async fn(t) {
        const {x: n, y: r, placement: o, rects: i, platform: s, elements: l, middlewareData: a} = t
          , {element: u, padding: d=0} = Kt(e, t) || {};
        if (u == null)
            return {};
        const f = $m(d)
          , c = {
            x: n,
            y: r
        }
          , y = cc(o)
          , w = uc(y)
          , x = await s.getDimensions(u)
          , E = y === "y"
          , h = E ? "top" : "left"
          , p = E ? "bottom" : "right"
          , v = E ? "clientHeight" : "clientWidth"
          , S = i.reference[w] + i.reference[y] - c[y] - i.floating[w]
          , C = c[y] - i.reference[y]
          , P = await (s.getOffsetParent == null ? void 0 : s.getOffsetParent(u));
        let b = P ? P[v] : 0;
        (!b || !await (s.isElement == null ? void 0 : s.isElement(P))) && (b = l.floating[v] || i.floating[w]);
        const N = S / 2 - C / 2
          , _ = b / 2 - x[w] / 2 - 1
          , O = An(f[h], _)
          , $ = An(f[p], _)
          , D = O
          , H = b - x[w] - $
          , L = b / 2 - x[w] / 2 + N
          , Q = qa(D, L, H)
          , U = !a.arrow && to(o) != null && L !== Q && i.reference[w] / 2 - (L < D ? O : $) - x[w] / 2 < 0
          , K = U ? L < D ? L - D : L - H : 0;
        return {
            [y]: c[y] + K,
            data: {
                [y]: Q,
                centerOffset: L - Q - K,
                ...U && {
                    alignmentOffset: K
                }
            },
            reset: U
        }
    }
})
  , h1 = function(e) {
    return e === void 0 && (e = {}),
    {
        name: "flip",
        options: e,
        async fn(t) {
            var n, r;
            const {placement: o, middlewareData: i, rects: s, initialPlacement: l, platform: a, elements: u} = t
              , {mainAxis: d=!0, crossAxis: f=!0, fallbackPlacements: c, fallbackStrategy: y="bestFit", fallbackAxisSideDirection: w="none", flipAlignment: x=!0, ...E} = Kt(e, t);
            if ((n = i.arrow) != null && n.alignmentOffset)
                return {};
            const h = Gt(o)
              , p = On(l)
              , v = Gt(l) === l
              , S = await (a.isRTL == null ? void 0 : a.isRTL(u.floating))
              , C = c || (v || !x ? [Ts(l)] : a1(l))
              , P = w !== "none";
            !c && P && C.push(...c1(l, x, w, S));
            const b = [l, ...C]
              , N = await qo(t, E)
              , _ = [];
            let O = ((r = i.flip) == null ? void 0 : r.overflows) || [];
            if (d && _.push(N[h]),
            f) {
                const L = l1(o, s, S);
                _.push(N[L[0]], N[L[1]])
            }
            if (O = [...O, {
                placement: o,
                overflows: _
            }],
            !_.every(L => L <= 0)) {
                var $, D;
                const L = ((($ = i.flip) == null ? void 0 : $.index) || 0) + 1
                  , Q = b[L];
                if (Q)
                    return {
                        data: {
                            index: L,
                            overflows: O
                        },
                        reset: {
                            placement: Q
                        }
                    };
                let U = (D = O.filter(K => K.overflows[0] <= 0).sort( (K, k) => K.overflows[1] - k.overflows[1])[0]) == null ? void 0 : D.placement;
                if (!U)
                    switch (y) {
                    case "bestFit":
                        {
                            var H;
                            const K = (H = O.filter(k => {
                                if (P) {
                                    const j = On(k.placement);
                                    return j === p || j === "y"
                                }
                                return !0
                            }
                            ).map(k => [k.placement, k.overflows.filter(j => j > 0).reduce( (j, z) => j + z, 0)]).sort( (k, j) => k[1] - j[1])[0]) == null ? void 0 : H[0];
                            K && (U = K);
                            break
                        }
                    case "initialPlacement":
                        U = l;
                        break
                    }
                if (o !== U)
                    return {
                        reset: {
                            placement: U
                        }
                    }
            }
            return {}
        }
    }
};
function ef(e, t) {
    return {
        top: e.top - t.height,
        right: e.right - t.width,
        bottom: e.bottom - t.height,
        left: e.left - t.width
    }
}
function tf(e) {
    return o1.some(t => e[t] >= 0)
}
const m1 = function(e) {
    return e === void 0 && (e = {}),
    {
        name: "hide",
        options: e,
        async fn(t) {
            const {rects: n} = t
              , {strategy: r="referenceHidden", ...o} = Kt(e, t);
            switch (r) {
            case "referenceHidden":
                {
                    const i = await qo(t, {
                        ...o,
                        elementContext: "reference"
                    })
                      , s = ef(i, n.reference);
                    return {
                        data: {
                            referenceHiddenOffsets: s,
                            referenceHidden: tf(s)
                        }
                    }
                }
            case "escaped":
                {
                    const i = await qo(t, {
                        ...o,
                        altBoundary: !0
                    })
                      , s = ef(i, n.floating);
                    return {
                        data: {
                            escapedOffsets: s,
                            escaped: tf(s)
                        }
                    }
                }
            default:
                return {}
            }
        }
    }
};
async function v1(e, t) {
    const {placement: n, platform: r, elements: o} = e
      , i = await (r.isRTL == null ? void 0 : r.isRTL(o.floating))
      , s = Gt(n)
      , l = to(n)
      , a = On(n) === "y"
      , u = ["left", "top"].includes(s) ? -1 : 1
      , d = i && a ? -1 : 1
      , f = Kt(t, e);
    let {mainAxis: c, crossAxis: y, alignmentAxis: w} = typeof f == "number" ? {
        mainAxis: f,
        crossAxis: 0,
        alignmentAxis: null
    } : {
        mainAxis: f.mainAxis || 0,
        crossAxis: f.crossAxis || 0,
        alignmentAxis: f.alignmentAxis
    };
    return l && typeof w == "number" && (y = l === "end" ? w * -1 : w),
    a ? {
        x: y * d,
        y: c * u
    } : {
        x: c * u,
        y: y * d
    }
}
const g1 = function(e) {
    return e === void 0 && (e = 0),
    {
        name: "offset",
        options: e,
        async fn(t) {
            var n, r;
            const {x: o, y: i, placement: s, middlewareData: l} = t
              , a = await v1(t, e);
            return s === ((n = l.offset) == null ? void 0 : n.placement) && (r = l.arrow) != null && r.alignmentOffset ? {} : {
                x: o + a.x,
                y: i + a.y,
                data: {
                    ...a,
                    placement: s
                }
            }
        }
    }
}
  , y1 = function(e) {
    return e === void 0 && (e = {}),
    {
        name: "shift",
        options: e,
        async fn(t) {
            const {x: n, y: r, placement: o} = t
              , {mainAxis: i=!0, crossAxis: s=!1, limiter: l={
                fn: E => {
                    let {x: h, y: p} = E;
                    return {
                        x: h,
                        y: p
                    }
                }
            }, ...a} = Kt(e, t)
              , u = {
                x: n,
                y: r
            }
              , d = await qo(t, a)
              , f = On(Gt(o))
              , c = ac(f);
            let y = u[c]
              , w = u[f];
            if (i) {
                const E = c === "y" ? "top" : "left"
                  , h = c === "y" ? "bottom" : "right"
                  , p = y + d[E]
                  , v = y - d[h];
                y = qa(p, y, v)
            }
            if (s) {
                const E = f === "y" ? "top" : "left"
                  , h = f === "y" ? "bottom" : "right"
                  , p = w + d[E]
                  , v = w - d[h];
                w = qa(p, w, v)
            }
            const x = l.fn({
                ...t,
                [c]: y,
                [f]: w
            });
            return {
                ...x,
                data: {
                    x: x.x - n,
                    y: x.y - r,
                    enabled: {
                        [c]: i,
                        [f]: s
                    }
                }
            }
        }
    }
}
  , x1 = function(e) {
    return e === void 0 && (e = {}),
    {
        options: e,
        fn(t) {
            const {x: n, y: r, placement: o, rects: i, middlewareData: s} = t
              , {offset: l=0, mainAxis: a=!0, crossAxis: u=!0} = Kt(e, t)
              , d = {
                x: n,
                y: r
            }
              , f = On(o)
              , c = ac(f);
            let y = d[c]
              , w = d[f];
            const x = Kt(l, t)
              , E = typeof x == "number" ? {
                mainAxis: x,
                crossAxis: 0
            } : {
                mainAxis: 0,
                crossAxis: 0,
                ...x
            };
            if (a) {
                const v = c === "y" ? "height" : "width"
                  , S = i.reference[c] - i.floating[v] + E.mainAxis
                  , C = i.reference[c] + i.reference[v] - E.mainAxis;
                y < S ? y = S : y > C && (y = C)
            }
            if (u) {
                var h, p;
                const v = c === "y" ? "width" : "height"
                  , S = ["top", "left"].includes(Gt(o))
                  , C = i.reference[f] - i.floating[v] + (S && ((h = s.offset) == null ? void 0 : h[f]) || 0) + (S ? 0 : E.crossAxis)
                  , P = i.reference[f] + i.reference[v] + (S ? 0 : ((p = s.offset) == null ? void 0 : p[f]) || 0) - (S ? E.crossAxis : 0);
                w < C ? w = C : w > P && (w = P)
            }
            return {
                [c]: y,
                [f]: w
            }
        }
    }
}
  , w1 = function(e) {
    return e === void 0 && (e = {}),
    {
        name: "size",
        options: e,
        async fn(t) {
            var n, r;
            const {placement: o, rects: i, platform: s, elements: l} = t
              , {apply: a= () => {}
            , ...u} = Kt(e, t)
              , d = await qo(t, u)
              , f = Gt(o)
              , c = to(o)
              , y = On(o) === "y"
              , {width: w, height: x} = i.floating;
            let E, h;
            f === "top" || f === "bottom" ? (E = f,
            h = c === (await (s.isRTL == null ? void 0 : s.isRTL(l.floating)) ? "start" : "end") ? "left" : "right") : (h = f,
            E = c === "end" ? "top" : "bottom");
            const p = x - d.top - d.bottom
              , v = w - d.left - d.right
              , S = An(x - d[E], p)
              , C = An(w - d[h], v)
              , P = !t.middlewareData.shift;
            let b = S
              , N = C;
            if ((n = t.middlewareData.shift) != null && n.enabled.x && (N = v),
            (r = t.middlewareData.shift) != null && r.enabled.y && (b = p),
            P && !c) {
                const O = Ge(d.left, 0)
                  , $ = Ge(d.right, 0)
                  , D = Ge(d.top, 0)
                  , H = Ge(d.bottom, 0);
                y ? N = w - 2 * (O !== 0 || $ !== 0 ? O + $ : Ge(d.left, d.right)) : b = x - 2 * (D !== 0 || H !== 0 ? D + H : Ge(d.top, d.bottom))
            }
            await a({
                ...t,
                availableWidth: N,
                availableHeight: b
            });
            const _ = await s.getDimensions(l.floating);
            return w !== _.width || x !== _.height ? {
                reset: {
                    rects: !0
                }
            } : {}
        }
    }
};
function tl() {
    return typeof window < "u"
}
function no(e) {
    return Um(e) ? (e.nodeName || "").toLowerCase() : "#document"
}
function qe(e) {
    var t;
    return (e == null || (t = e.ownerDocument) == null ? void 0 : t.defaultView) || window
}
function Mt(e) {
    var t;
    return (t = (Um(e) ? e.ownerDocument : e.document) || window.document) == null ? void 0 : t.documentElement
}
function Um(e) {
    return tl() ? e instanceof Node || e instanceof qe(e).Node : !1
}
function wt(e) {
    return tl() ? e instanceof Element || e instanceof qe(e).Element : !1
}
function Lt(e) {
    return tl() ? e instanceof HTMLElement || e instanceof qe(e).HTMLElement : !1
}
function nf(e) {
    return !tl() || typeof ShadowRoot > "u" ? !1 : e instanceof ShadowRoot || e instanceof qe(e).ShadowRoot
}
function di(e) {
    const {overflow: t, overflowX: n, overflowY: r, display: o} = Et(e);
    return /auto|scroll|overlay|hidden|clip/.test(t + r + n) && !["inline", "contents"].includes(o)
}
function E1(e) {
    return ["table", "td", "th"].includes(no(e))
}
function nl(e) {
    return [":popover-open", ":modal"].some(t => {
        try {
            return e.matches(t)
        } catch {
            return !1
        }
    }
    )
}
function dc(e) {
    const t = fc()
      , n = wt(e) ? Et(e) : e;
    return n.transform !== "none" || n.perspective !== "none" || (n.containerType ? n.containerType !== "normal" : !1) || !t && (n.backdropFilter ? n.backdropFilter !== "none" : !1) || !t && (n.filter ? n.filter !== "none" : !1) || ["transform", "perspective", "filter"].some(r => (n.willChange || "").includes(r)) || ["paint", "layout", "strict", "content"].some(r => (n.contain || "").includes(r))
}
function S1(e) {
    let t = _n(e);
    for (; Lt(t) && !Gr(t); ) {
        if (dc(t))
            return t;
        if (nl(t))
            return null;
        t = _n(t)
    }
    return null
}
function fc() {
    return typeof CSS > "u" || !CSS.supports ? !1 : CSS.supports("-webkit-backdrop-filter", "none")
}
function Gr(e) {
    return ["html", "body", "#document"].includes(no(e))
}
function Et(e) {
    return qe(e).getComputedStyle(e)
}
function rl(e) {
    return wt(e) ? {
        scrollLeft: e.scrollLeft,
        scrollTop: e.scrollTop
    } : {
        scrollLeft: e.scrollX,
        scrollTop: e.scrollY
    }
}
function _n(e) {
    if (no(e) === "html")
        return e;
    const t = e.assignedSlot || e.parentNode || nf(e) && e.host || Mt(e);
    return nf(t) ? t.host : t
}
function Bm(e) {
    const t = _n(e);
    return Gr(t) ? e.ownerDocument ? e.ownerDocument.body : e.body : Lt(t) && di(t) ? t : Bm(t)
}
function Zo(e, t, n) {
    var r;
    t === void 0 && (t = []),
    n === void 0 && (n = !0);
    const o = Bm(e)
      , i = o === ((r = e.ownerDocument) == null ? void 0 : r.body)
      , s = qe(o);
    if (i) {
        const l = Ja(s);
        return t.concat(s, s.visualViewport || [], di(o) ? o : [], l && n ? Zo(l) : [])
    }
    return t.concat(o, Zo(o, [], n))
}
function Ja(e) {
    return e.parent && Object.getPrototypeOf(e.parent) ? e.frameElement : null
}
function Wm(e) {
    const t = Et(e);
    let n = parseFloat(t.width) || 0
      , r = parseFloat(t.height) || 0;
    const o = Lt(e)
      , i = o ? e.offsetWidth : n
      , s = o ? e.offsetHeight : r
      , l = Ns(n) !== i || Ns(r) !== s;
    return l && (n = i,
    r = s),
    {
        width: n,
        height: r,
        $: l
    }
}
function pc(e) {
    return wt(e) ? e : e.contextElement
}
function jr(e) {
    const t = pc(e);
    if (!Lt(t))
        return jn(1);
    const n = t.getBoundingClientRect()
      , {width: r, height: o, $: i} = Wm(t);
    let s = (i ? Ns(n.width) : n.width) / r
      , l = (i ? Ns(n.height) : n.height) / o;
    return (!s || !Number.isFinite(s)) && (s = 1),
    (!l || !Number.isFinite(l)) && (l = 1),
    {
        x: s,
        y: l
    }
}
const C1 = jn(0);
function Vm(e) {
    const t = qe(e);
    return !fc() || !t.visualViewport ? C1 : {
        x: t.visualViewport.offsetLeft,
        y: t.visualViewport.offsetTop
    }
}
function b1(e, t, n) {
    return t === void 0 && (t = !1),
    !n || t && n !== qe(e) ? !1 : t
}
function nr(e, t, n, r) {
    t === void 0 && (t = !1),
    n === void 0 && (n = !1);
    const o = e.getBoundingClientRect()
      , i = pc(e);
    let s = jn(1);
    t && (r ? wt(r) && (s = jr(r)) : s = jr(e));
    const l = b1(i, n, r) ? Vm(i) : jn(0);
    let a = (o.left + l.x) / s.x
      , u = (o.top + l.y) / s.y
      , d = o.width / s.x
      , f = o.height / s.y;
    if (i) {
        const c = qe(i)
          , y = r && wt(r) ? qe(r) : r;
        let w = c
          , x = Ja(w);
        for (; x && r && y !== w; ) {
            const E = jr(x)
              , h = x.getBoundingClientRect()
              , p = Et(x)
              , v = h.left + (x.clientLeft + parseFloat(p.paddingLeft)) * E.x
              , S = h.top + (x.clientTop + parseFloat(p.paddingTop)) * E.y;
            a *= E.x,
            u *= E.y,
            d *= E.x,
            f *= E.y,
            a += v,
            u += S,
            w = qe(x),
            x = Ja(w)
        }
    }
    return Rs({
        width: d,
        height: f,
        x: a,
        y: u
    })
}
function k1(e) {
    let {elements: t, rect: n, offsetParent: r, strategy: o} = e;
    const i = o === "fixed"
      , s = Mt(r)
      , l = t ? nl(t.floating) : !1;
    if (r === s || l && i)
        return n;
    let a = {
        scrollLeft: 0,
        scrollTop: 0
    }
      , u = jn(1);
    const d = jn(0)
      , f = Lt(r);
    if ((f || !f && !i) && ((no(r) !== "body" || di(s)) && (a = rl(r)),
    Lt(r))) {
        const c = nr(r);
        u = jr(r),
        d.x = c.x + r.clientLeft,
        d.y = c.y + r.clientTop
    }
    return {
        width: n.width * u.x,
        height: n.height * u.y,
        x: n.x * u.x - a.scrollLeft * u.x + d.x,
        y: n.y * u.y - a.scrollTop * u.y + d.y
    }
}
function P1(e) {
    return Array.from(e.getClientRects())
}
function eu(e, t) {
    const n = rl(e).scrollLeft;
    return t ? t.left + n : nr(Mt(e)).left + n
}
function N1(e) {
    const t = Mt(e)
      , n = rl(e)
      , r = e.ownerDocument.body
      , o = Ge(t.scrollWidth, t.clientWidth, r.scrollWidth, r.clientWidth)
      , i = Ge(t.scrollHeight, t.clientHeight, r.scrollHeight, r.clientHeight);
    let s = -n.scrollLeft + eu(e);
    const l = -n.scrollTop;
    return Et(r).direction === "rtl" && (s += Ge(t.clientWidth, r.clientWidth) - o),
    {
        width: o,
        height: i,
        x: s,
        y: l
    }
}
function T1(e, t) {
    const n = qe(e)
      , r = Mt(e)
      , o = n.visualViewport;
    let i = r.clientWidth
      , s = r.clientHeight
      , l = 0
      , a = 0;
    if (o) {
        i = o.width,
        s = o.height;
        const u = fc();
        (!u || u && t === "fixed") && (l = o.offsetLeft,
        a = o.offsetTop)
    }
    return {
        width: i,
        height: s,
        x: l,
        y: a
    }
}
function R1(e, t) {
    const n = nr(e, !0, t === "fixed")
      , r = n.top + e.clientTop
      , o = n.left + e.clientLeft
      , i = Lt(e) ? jr(e) : jn(1)
      , s = e.clientWidth * i.x
      , l = e.clientHeight * i.y
      , a = o * i.x
      , u = r * i.y;
    return {
        width: s,
        height: l,
        x: a,
        y: u
    }
}
function rf(e, t, n) {
    let r;
    if (t === "viewport")
        r = T1(e, n);
    else if (t === "document")
        r = N1(Mt(e));
    else if (wt(t))
        r = R1(t, n);
    else {
        const o = Vm(e);
        r = {
            ...t,
            x: t.x - o.x,
            y: t.y - o.y
        }
    }
    return Rs(r)
}
function Hm(e, t) {
    const n = _n(e);
    return n === t || !wt(n) || Gr(n) ? !1 : Et(n).position === "fixed" || Hm(n, t)
}
function A1(e, t) {
    const n = t.get(e);
    if (n)
        return n;
    let r = Zo(e, [], !1).filter(l => wt(l) && no(l) !== "body")
      , o = null;
    const i = Et(e).position === "fixed";
    let s = i ? _n(e) : e;
    for (; wt(s) && !Gr(s); ) {
        const l = Et(s)
          , a = dc(s);
        !a && l.position === "fixed" && (o = null),
        (i ? !a && !o : !a && l.position === "static" && !!o && ["absolute", "fixed"].includes(o.position) || di(s) && !a && Hm(e, s)) ? r = r.filter(d => d !== s) : o = l,
        s = _n(s)
    }
    return t.set(e, r),
    r
}
function j1(e) {
    let {element: t, boundary: n, rootBoundary: r, strategy: o} = e;
    const s = [...n === "clippingAncestors" ? nl(t) ? [] : A1(t, this._c) : [].concat(n), r]
      , l = s[0]
      , a = s.reduce( (u, d) => {
        const f = rf(t, d, o);
        return u.top = Ge(f.top, u.top),
        u.right = An(f.right, u.right),
        u.bottom = An(f.bottom, u.bottom),
        u.left = Ge(f.left, u.left),
        u
    }
    , rf(t, l, o));
    return {
        width: a.right - a.left,
        height: a.bottom - a.top,
        x: a.left,
        y: a.top
    }
}
function O1(e) {
    const {width: t, height: n} = Wm(e);
    return {
        width: t,
        height: n
    }
}
function _1(e, t, n) {
    const r = Lt(t)
      , o = Mt(t)
      , i = n === "fixed"
      , s = nr(e, !0, i, t);
    let l = {
        scrollLeft: 0,
        scrollTop: 0
    };
    const a = jn(0);
    if (r || !r && !i)
        if ((no(t) !== "body" || di(o)) && (l = rl(t)),
        r) {
            const y = nr(t, !0, i, t);
            a.x = y.x + t.clientLeft,
            a.y = y.y + t.clientTop
        } else
            o && (a.x = eu(o));
    let u = 0
      , d = 0;
    if (o && !r && !i) {
        const y = o.getBoundingClientRect();
        d = y.top + l.scrollTop,
        u = y.left + l.scrollLeft - eu(o, y)
    }
    const f = s.left + l.scrollLeft - a.x - u
      , c = s.top + l.scrollTop - a.y - d;
    return {
        x: f,
        y: c,
        width: s.width,
        height: s.height
    }
}
function Kl(e) {
    return Et(e).position === "static"
}
function of(e, t) {
    if (!Lt(e) || Et(e).position === "fixed")
        return null;
    if (t)
        return t(e);
    let n = e.offsetParent;
    return Mt(e) === n && (n = n.ownerDocument.body),
    n
}
function Qm(e, t) {
    const n = qe(e);
    if (nl(e))
        return n;
    if (!Lt(e)) {
        let o = _n(e);
        for (; o && !Gr(o); ) {
            if (wt(o) && !Kl(o))
                return o;
            o = _n(o)
        }
        return n
    }
    let r = of(e, t);
    for (; r && E1(r) && Kl(r); )
        r = of(r, t);
    return r && Gr(r) && Kl(r) && !dc(r) ? n : r || S1(e) || n
}
const L1 = async function(e) {
    const t = this.getOffsetParent || Qm
      , n = this.getDimensions
      , r = await n(e.floating);
    return {
        reference: _1(e.reference, await t(e.floating), e.strategy),
        floating: {
            x: 0,
            y: 0,
            width: r.width,
            height: r.height
        }
    }
};
function M1(e) {
    return Et(e).direction === "rtl"
}
const I1 = {
    convertOffsetParentRelativeRectToViewportRelativeRect: k1,
    getDocumentElement: Mt,
    getClippingRect: j1,
    getOffsetParent: Qm,
    getElementRects: L1,
    getClientRects: P1,
    getDimensions: O1,
    getScale: jr,
    isElement: wt,
    isRTL: M1
};
function D1(e, t) {
    let n = null, r;
    const o = Mt(e);
    function i() {
        var l;
        clearTimeout(r),
        (l = n) == null || l.disconnect(),
        n = null
    }
    function s(l, a) {
        l === void 0 && (l = !1),
        a === void 0 && (a = 1),
        i();
        const {left: u, top: d, width: f, height: c} = e.getBoundingClientRect();
        if (l || t(),
        !f || !c)
            return;
        const y = Fi(d)
          , w = Fi(o.clientWidth - (u + f))
          , x = Fi(o.clientHeight - (d + c))
          , E = Fi(u)
          , p = {
            rootMargin: -y + "px " + -w + "px " + -x + "px " + -E + "px",
            threshold: Ge(0, An(1, a)) || 1
        };
        let v = !0;
        function S(C) {
            const P = C[0].intersectionRatio;
            if (P !== a) {
                if (!v)
                    return s();
                P ? s(!1, P) : r = setTimeout( () => {
                    s(!1, 1e-7)
                }
                , 1e3)
            }
            v = !1
        }
        try {
            n = new IntersectionObserver(S,{
                ...p,
                root: o.ownerDocument
            })
        } catch {
            n = new IntersectionObserver(S,p)
        }
        n.observe(e)
    }
    return s(!0),
    i
}
function z1(e, t, n, r) {
    r === void 0 && (r = {});
    const {ancestorScroll: o=!0, ancestorResize: i=!0, elementResize: s=typeof ResizeObserver == "function", layoutShift: l=typeof IntersectionObserver == "function", animationFrame: a=!1} = r
      , u = pc(e)
      , d = o || i ? [...u ? Zo(u) : [], ...Zo(t)] : [];
    d.forEach(h => {
        o && h.addEventListener("scroll", n, {
            passive: !0
        }),
        i && h.addEventListener("resize", n)
    }
    );
    const f = u && l ? D1(u, n) : null;
    let c = -1
      , y = null;
    s && (y = new ResizeObserver(h => {
        let[p] = h;
        p && p.target === u && y && (y.unobserve(t),
        cancelAnimationFrame(c),
        c = requestAnimationFrame( () => {
            var v;
            (v = y) == null || v.observe(t)
        }
        )),
        n()
    }
    ),
    u && !a && y.observe(u),
    y.observe(t));
    let w, x = a ? nr(e) : null;
    a && E();
    function E() {
        const h = nr(e);
        x && (h.x !== x.x || h.y !== x.y || h.width !== x.width || h.height !== x.height) && n(),
        x = h,
        w = requestAnimationFrame(E)
    }
    return n(),
    () => {
        var h;
        d.forEach(p => {
            o && p.removeEventListener("scroll", n),
            i && p.removeEventListener("resize", n)
        }
        ),
        f == null || f(),
        (h = y) == null || h.disconnect(),
        y = null,
        a && cancelAnimationFrame(w)
    }
}
const F1 = g1
  , $1 = y1
  , U1 = h1
  , B1 = w1
  , W1 = m1
  , sf = p1
  , V1 = x1
  , H1 = (e, t, n) => {
    const r = new Map
      , o = {
        platform: I1,
        ...n
    }
      , i = {
        ...o.platform,
        _c: r
    };
    return f1(e, t, {
        ...o,
        platform: i
    })
}
;
var ns = typeof document < "u" ? g.useLayoutEffect : g.useEffect;
function As(e, t) {
    if (e === t)
        return !0;
    if (typeof e != typeof t)
        return !1;
    if (typeof e == "function" && e.toString() === t.toString())
        return !0;
    let n, r, o;
    if (e && t && typeof e == "object") {
        if (Array.isArray(e)) {
            if (n = e.length,
            n !== t.length)
                return !1;
            for (r = n; r-- !== 0; )
                if (!As(e[r], t[r]))
                    return !1;
            return !0
        }
        if (o = Object.keys(e),
        n = o.length,
        n !== Object.keys(t).length)
            return !1;
        for (r = n; r-- !== 0; )
            if (!{}.hasOwnProperty.call(t, o[r]))
                return !1;
        for (r = n; r-- !== 0; ) {
            const i = o[r];
            if (!(i === "_owner" && e.$$typeof) && !As(e[i], t[i]))
                return !1
        }
        return !0
    }
    return e !== e && t !== t
}
function Km(e) {
    return typeof window > "u" ? 1 : (e.ownerDocument.defaultView || window).devicePixelRatio || 1
}
function lf(e, t) {
    const n = Km(e);
    return Math.round(t * n) / n
}
function Gl(e) {
    const t = g.useRef(e);
    return ns( () => {
        t.current = e
    }
    ),
    t
}
function Q1(e) {
    e === void 0 && (e = {});
    const {placement: t="bottom", strategy: n="absolute", middleware: r=[], platform: o, elements: {reference: i, floating: s}={}, transform: l=!0, whileElementsMounted: a, open: u} = e
      , [d,f] = g.useState({
        x: 0,
        y: 0,
        strategy: n,
        placement: t,
        middlewareData: {},
        isPositioned: !1
    })
      , [c,y] = g.useState(r);
    As(c, r) || y(r);
    const [w,x] = g.useState(null)
      , [E,h] = g.useState(null)
      , p = g.useCallback(k => {
        k !== P.current && (P.current = k,
        x(k))
    }
    , [])
      , v = g.useCallback(k => {
        k !== b.current && (b.current = k,
        h(k))
    }
    , [])
      , S = i || w
      , C = s || E
      , P = g.useRef(null)
      , b = g.useRef(null)
      , N = g.useRef(d)
      , _ = a != null
      , O = Gl(a)
      , $ = Gl(o)
      , D = Gl(u)
      , H = g.useCallback( () => {
        if (!P.current || !b.current)
            return;
        const k = {
            placement: t,
            strategy: n,
            middleware: c
        };
        $.current && (k.platform = $.current),
        H1(P.current, b.current, k).then(j => {
            const z = {
                ...j,
                isPositioned: D.current !== !1
            };
            L.current && !As(N.current, z) && (N.current = z,
            Jr.flushSync( () => {
                f(z)
            }
            ))
        }
        )
    }
    , [c, t, n, $, D]);
    ns( () => {
        u === !1 && N.current.isPositioned && (N.current.isPositioned = !1,
        f(k => ({
            ...k,
            isPositioned: !1
        })))
    }
    , [u]);
    const L = g.useRef(!1);
    ns( () => (L.current = !0,
    () => {
        L.current = !1
    }
    ), []),
    ns( () => {
        if (S && (P.current = S),
        C && (b.current = C),
        S && C) {
            if (O.current)
                return O.current(S, C, H);
            H()
        }
    }
    , [S, C, H, O, _]);
    const Q = g.useMemo( () => ({
        reference: P,
        floating: b,
        setReference: p,
        setFloating: v
    }), [p, v])
      , U = g.useMemo( () => ({
        reference: S,
        floating: C
    }), [S, C])
      , K = g.useMemo( () => {
        const k = {
            position: n,
            left: 0,
            top: 0
        };
        if (!U.floating)
            return k;
        const j = lf(U.floating, d.x)
          , z = lf(U.floating, d.y);
        return l ? {
            ...k,
            transform: "translate(" + j + "px, " + z + "px)",
            ...Km(U.floating) >= 1.5 && {
                willChange: "transform"
            }
        } : {
            position: n,
            left: j,
            top: z
        }
    }
    , [n, l, U.floating, d.x, d.y]);
    return g.useMemo( () => ({
        ...d,
        update: H,
        refs: Q,
        elements: U,
        floatingStyles: K
    }), [d, H, Q, U, K])
}
const K1 = e => {
    function t(n) {
        return {}.hasOwnProperty.call(n, "current")
    }
    return {
        name: "arrow",
        options: e,
        fn(n) {
            const {element: r, padding: o} = typeof e == "function" ? e(n) : e;
            return r && t(r) ? r.current != null ? sf({
                element: r.current,
                padding: o
            }).fn(n) : {} : r ? sf({
                element: r,
                padding: o
            }).fn(n) : {}
        }
    }
}
  , G1 = (e, t) => ({
    ...F1(e),
    options: [e, t]
})
  , Y1 = (e, t) => ({
    ...$1(e),
    options: [e, t]
})
  , X1 = (e, t) => ({
    ...V1(e),
    options: [e, t]
})
  , q1 = (e, t) => ({
    ...U1(e),
    options: [e, t]
})
  , Z1 = (e, t) => ({
    ...B1(e),
    options: [e, t]
})
  , J1 = (e, t) => ({
    ...W1(e),
    options: [e, t]
})
  , eE = (e, t) => ({
    ...K1(e),
    options: [e, t]
});
var tE = "Arrow"
  , Gm = g.forwardRef( (e, t) => {
    const {children: n, width: r=10, height: o=5, ...i} = e;
    return m.jsx(me.svg, {
        ...i,
        ref: t,
        width: r,
        height: o,
        viewBox: "0 0 30 10",
        preserveAspectRatio: "none",
        children: e.asChild ? n : m.jsx("polygon", {
            points: "0,0 30,0 15,10"
        })
    })
}
);
Gm.displayName = tE;
var nE = Gm;
function rE(e) {
    const [t,n] = g.useState(void 0);
    return xt( () => {
        if (e) {
            n({
                width: e.offsetWidth,
                height: e.offsetHeight
            });
            const r = new ResizeObserver(o => {
                if (!Array.isArray(o) || !o.length)
                    return;
                const i = o[0];
                let s, l;
                if ("borderBoxSize"in i) {
                    const a = i.borderBoxSize
                      , u = Array.isArray(a) ? a[0] : a;
                    s = u.inlineSize,
                    l = u.blockSize
                } else
                    s = e.offsetWidth,
                    l = e.offsetHeight;
                n({
                    width: s,
                    height: l
                })
            }
            );
            return r.observe(e, {
                box: "border-box"
            }),
            () => r.unobserve(e)
        } else
            n(void 0)
    }
    , [e]),
    t
}
var Ym = "Popper"
  , [Xm,qm] = ci(Ym)
  , [FC,Zm] = Xm(Ym)
  , Jm = "PopperAnchor"
  , ev = g.forwardRef( (e, t) => {
    const {__scopePopper: n, virtualRef: r, ...o} = e
      , i = Zm(Jm, n)
      , s = g.useRef(null)
      , l = Oe(t, s);
    return g.useEffect( () => {
        i.onAnchorChange((r == null ? void 0 : r.current) || s.current)
    }
    ),
    r ? null : m.jsx(me.div, {
        ...o,
        ref: l
    })
}
);
ev.displayName = Jm;
var hc = "PopperContent"
  , [oE,iE] = Xm(hc)
  , tv = g.forwardRef( (e, t) => {
    var ut, qt, Zt, Jt, hi, lr;
    const {__scopePopper: n, side: r="bottom", sideOffset: o=0, align: i="center", alignOffset: s=0, arrowPadding: l=0, avoidCollisions: a=!0, collisionBoundary: u=[], collisionPadding: d=0, sticky: f="partial", hideWhenDetached: c=!1, updatePositionStrategy: y="optimized", onPlaced: w, ...x} = e
      , E = Zm(hc, n)
      , [h,p] = g.useState(null)
      , v = Oe(t, Dn => p(Dn))
      , [S,C] = g.useState(null)
      , P = rE(S)
      , b = (P == null ? void 0 : P.width) ?? 0
      , N = (P == null ? void 0 : P.height) ?? 0
      , _ = r + (i !== "center" ? "-" + i : "")
      , O = typeof d == "number" ? d : {
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        ...d
    }
      , $ = Array.isArray(u) ? u : [u]
      , D = $.length > 0
      , H = {
        padding: O,
        boundary: $.filter(lE),
        altBoundary: D
    }
      , {refs: L, floatingStyles: Q, placement: U, isPositioned: K, middlewareData: k} = Q1({
        strategy: "fixed",
        placement: _,
        whileElementsMounted: (...Dn) => z1(...Dn, {
            animationFrame: y === "always"
        }),
        elements: {
            reference: E.anchor
        },
        middleware: [G1({
            mainAxis: o + N,
            alignmentAxis: s
        }), a && Y1({
            mainAxis: !0,
            crossAxis: !1,
            limiter: f === "partial" ? X1() : void 0,
            ...H
        }), a && q1({
            ...H
        }), Z1({
            ...H,
            apply: ({elements: Dn, rects: so, availableWidth: mi, availableHeight: en}) => {
                const {width: fl, height: pl} = so.reference
                  , Se = Dn.floating.style;
                Se.setProperty("--radix-popper-available-width", `${mi}px`),
                Se.setProperty("--radix-popper-available-height", `${en}px`),
                Se.setProperty("--radix-popper-anchor-width", `${fl}px`),
                Se.setProperty("--radix-popper-anchor-height", `${pl}px`)
            }
        }), S && eE({
            element: S,
            padding: l
        }), aE({
            arrowWidth: b,
            arrowHeight: N
        }), c && J1({
            strategy: "referenceHidden",
            ...H
        })]
    })
      , [j,z] = ov(U)
      , I = at(w);
    xt( () => {
        K && (I == null || I())
    }
    , [K, I]);
    const F = (ut = k.arrow) == null ? void 0 : ut.x
      , Y = (qt = k.arrow) == null ? void 0 : qt.y
      , ae = ((Zt = k.arrow) == null ? void 0 : Zt.centerOffset) !== 0
      , [Ve,Z] = g.useState();
    return xt( () => {
        h && Z(window.getComputedStyle(h).zIndex)
    }
    , [h]),
    m.jsx("div", {
        ref: L.setFloating,
        "data-radix-popper-content-wrapper": "",
        style: {
            ...Q,
            transform: K ? Q.transform : "translate(0, -200%)",
            minWidth: "max-content",
            zIndex: Ve,
            "--radix-popper-transform-origin": [(Jt = k.transformOrigin) == null ? void 0 : Jt.x, (hi = k.transformOrigin) == null ? void 0 : hi.y].join(" "),
            ...((lr = k.hide) == null ? void 0 : lr.referenceHidden) && {
                visibility: "hidden",
                pointerEvents: "none"
            }
        },
        dir: e.dir,
        children: m.jsx(oE, {
            scope: n,
            placedSide: j,
            onArrowChange: C,
            arrowX: F,
            arrowY: Y,
            shouldHideArrow: ae,
            children: m.jsx(me.div, {
                "data-side": j,
                "data-align": z,
                ...x,
                ref: v,
                style: {
                    ...x.style,
                    animation: K ? void 0 : "none"
                }
            })
        })
    })
}
);
tv.displayName = hc;
var nv = "PopperArrow"
  , sE = {
    top: "bottom",
    right: "left",
    bottom: "top",
    left: "right"
}
  , rv = g.forwardRef(function(t, n) {
    const {__scopePopper: r, ...o} = t
      , i = iE(nv, r)
      , s = sE[i.placedSide];
    return m.jsx("span", {
        ref: i.onArrowChange,
        style: {
            position: "absolute",
            left: i.arrowX,
            top: i.arrowY,
            [s]: 0,
            transformOrigin: {
                top: "",
                right: "0 0",
                bottom: "center 0",
                left: "100% 0"
            }[i.placedSide],
            transform: {
                top: "translateY(100%)",
                right: "translateY(50%) rotate(90deg) translateX(-50%)",
                bottom: "rotate(180deg)",
                left: "translateY(50%) rotate(-90deg) translateX(50%)"
            }[i.placedSide],
            visibility: i.shouldHideArrow ? "hidden" : void 0
        },
        children: m.jsx(nE, {
            ...o,
            ref: n,
            style: {
                ...o.style,
                display: "block"
            }
        })
    })
});
rv.displayName = nv;
function lE(e) {
    return e !== null
}
var aE = e => ({
    name: "transformOrigin",
    options: e,
    fn(t) {
        var E, h, p;
        const {placement: n, rects: r, middlewareData: o} = t
          , s = ((E = o.arrow) == null ? void 0 : E.centerOffset) !== 0
          , l = s ? 0 : e.arrowWidth
          , a = s ? 0 : e.arrowHeight
          , [u,d] = ov(n)
          , f = {
            start: "0%",
            center: "50%",
            end: "100%"
        }[d]
          , c = (((h = o.arrow) == null ? void 0 : h.x) ?? 0) + l / 2
          , y = (((p = o.arrow) == null ? void 0 : p.y) ?? 0) + a / 2;
        let w = ""
          , x = "";
        return u === "bottom" ? (w = s ? f : `${c}px`,
        x = `${-a}px`) : u === "top" ? (w = s ? f : `${c}px`,
        x = `${r.floating.height + a}px`) : u === "right" ? (w = `${-a}px`,
        x = s ? f : `${y}px`) : u === "left" && (w = `${r.floating.width + a}px`,
        x = s ? f : `${y}px`),
        {
            data: {
                x: w,
                y: x
            }
        }
    }
});
function ov(e) {
    const [t,n="center"] = e.split("-");
    return [t, n]
}
var uE = ev
  , cE = tv
  , dE = rv;
function fE(e, t) {
    return g.useReducer( (n, r) => t[n][r] ?? n, e)
}
var iv = e => {
    const {present: t, children: n} = e
      , r = pE(t)
      , o = typeof n == "function" ? n({
        present: r.isPresent
    }) : g.Children.only(n)
      , i = Oe(r.ref, hE(o));
    return typeof n == "function" || r.isPresent ? g.cloneElement(o, {
        ref: i
    }) : null
}
;
iv.displayName = "Presence";
function pE(e) {
    const [t,n] = g.useState()
      , r = g.useRef({})
      , o = g.useRef(e)
      , i = g.useRef("none")
      , s = e ? "mounted" : "unmounted"
      , [l,a] = fE(s, {
        mounted: {
            UNMOUNT: "unmounted",
            ANIMATION_OUT: "unmountSuspended"
        },
        unmountSuspended: {
            MOUNT: "mounted",
            ANIMATION_END: "unmounted"
        },
        unmounted: {
            MOUNT: "mounted"
        }
    });
    return g.useEffect( () => {
        const u = $i(r.current);
        i.current = l === "mounted" ? u : "none"
    }
    , [l]),
    xt( () => {
        const u = r.current
          , d = o.current;
        if (d !== e) {
            const c = i.current
              , y = $i(u);
            e ? a("MOUNT") : y === "none" || (u == null ? void 0 : u.display) === "none" ? a("UNMOUNT") : a(d && c !== y ? "ANIMATION_OUT" : "UNMOUNT"),
            o.current = e
        }
    }
    , [e, a]),
    xt( () => {
        if (t) {
            let u;
            const d = t.ownerDocument.defaultView ?? window
              , f = y => {
                const x = $i(r.current).includes(y.animationName);
                if (y.target === t && x && (a("ANIMATION_END"),
                !o.current)) {
                    const E = t.style.animationFillMode;
                    t.style.animationFillMode = "forwards",
                    u = d.setTimeout( () => {
                        t.style.animationFillMode === "forwards" && (t.style.animationFillMode = E)
                    }
                    )
                }
            }
              , c = y => {
                y.target === t && (i.current = $i(r.current))
            }
            ;
            return t.addEventListener("animationstart", c),
            t.addEventListener("animationcancel", f),
            t.addEventListener("animationend", f),
            () => {
                d.clearTimeout(u),
                t.removeEventListener("animationstart", c),
                t.removeEventListener("animationcancel", f),
                t.removeEventListener("animationend", f)
            }
        } else
            a("ANIMATION_END")
    }
    , [t, a]),
    {
        isPresent: ["mounted", "unmountSuspended"].includes(l),
        ref: g.useCallback(u => {
            u && (r.current = getComputedStyle(u)),
            n(u)
        }
        , [])
    }
}
function $i(e) {
    return (e == null ? void 0 : e.animationName) || "none"
}
function hE(e) {
    var r, o;
    let t = (r = Object.getOwnPropertyDescriptor(e.props, "ref")) == null ? void 0 : r.get
      , n = t && "isReactWarning"in t && t.isReactWarning;
    return n ? e.ref : (t = (o = Object.getOwnPropertyDescriptor(e, "ref")) == null ? void 0 : o.get,
    n = t && "isReactWarning"in t && t.isReactWarning,
    n ? e.props.ref : e.props.ref || e.ref)
}
var [ol,$C] = Kw("Tooltip", [qm])
  , mc = qm()
  , sv = "TooltipProvider"
  , mE = 700
  , af = "tooltip.open"
  , [vE,lv] = ol(sv)
  , av = e => {
    const {__scopeTooltip: t, delayDuration: n=mE, skipDelayDuration: r=300, disableHoverableContent: o=!1, children: i} = e
      , [s,l] = g.useState(!0)
      , a = g.useRef(!1)
      , u = g.useRef(0);
    return g.useEffect( () => {
        const d = u.current;
        return () => window.clearTimeout(d)
    }
    , []),
    m.jsx(vE, {
        scope: t,
        isOpenDelayed: s,
        delayDuration: n,
        onOpen: g.useCallback( () => {
            window.clearTimeout(u.current),
            l(!1)
        }
        , []),
        onClose: g.useCallback( () => {
            window.clearTimeout(u.current),
            u.current = window.setTimeout( () => l(!0), r)
        }
        , [r]),
        isPointerInTransitRef: a,
        onPointerInTransitChange: g.useCallback(d => {
            a.current = d
        }
        , []),
        disableHoverableContent: o,
        children: i
    })
}
;
av.displayName = sv;
var uv = "Tooltip"
  , [UC,il] = ol(uv)
  , tu = "TooltipTrigger"
  , gE = g.forwardRef( (e, t) => {
    const {__scopeTooltip: n, ...r} = e
      , o = il(tu, n)
      , i = lv(tu, n)
      , s = mc(n)
      , l = g.useRef(null)
      , a = Oe(t, l, o.onTriggerChange)
      , u = g.useRef(!1)
      , d = g.useRef(!1)
      , f = g.useCallback( () => u.current = !1, []);
    return g.useEffect( () => () => document.removeEventListener("pointerup", f), [f]),
    m.jsx(uE, {
        asChild: !0,
        ...s,
        children: m.jsx(me.button, {
            "aria-describedby": o.open ? o.contentId : void 0,
            "data-state": o.stateAttribute,
            ...r,
            ref: a,
            onPointerMove: le(e.onPointerMove, c => {
                c.pointerType !== "touch" && !d.current && !i.isPointerInTransitRef.current && (o.onTriggerEnter(),
                d.current = !0)
            }
            ),
            onPointerLeave: le(e.onPointerLeave, () => {
                o.onTriggerLeave(),
                d.current = !1
            }
            ),
            onPointerDown: le(e.onPointerDown, () => {
                u.current = !0,
                document.addEventListener("pointerup", f, {
                    once: !0
                })
            }
            ),
            onFocus: le(e.onFocus, () => {
                u.current || o.onOpen()
            }
            ),
            onBlur: le(e.onBlur, o.onClose),
            onClick: le(e.onClick, o.onClose)
        })
    })
}
);
gE.displayName = tu;
var yE = "TooltipPortal"
  , [BC,xE] = ol(yE, {
    forceMount: void 0
})
  , Yr = "TooltipContent"
  , cv = g.forwardRef( (e, t) => {
    const n = xE(Yr, e.__scopeTooltip)
      , {forceMount: r=n.forceMount, side: o="top", ...i} = e
      , s = il(Yr, e.__scopeTooltip);
    return m.jsx(iv, {
        present: r || s.open,
        children: s.disableHoverableContent ? m.jsx(dv, {
            side: o,
            ...i,
            ref: t
        }) : m.jsx(wE, {
            side: o,
            ...i,
            ref: t
        })
    })
}
)
  , wE = g.forwardRef( (e, t) => {
    const n = il(Yr, e.__scopeTooltip)
      , r = lv(Yr, e.__scopeTooltip)
      , o = g.useRef(null)
      , i = Oe(t, o)
      , [s,l] = g.useState(null)
      , {trigger: a, onClose: u} = n
      , d = o.current
      , {onPointerInTransitChange: f} = r
      , c = g.useCallback( () => {
        l(null),
        f(!1)
    }
    , [f])
      , y = g.useCallback( (w, x) => {
        const E = w.currentTarget
          , h = {
            x: w.clientX,
            y: w.clientY
        }
          , p = bE(h, E.getBoundingClientRect())
          , v = kE(h, p)
          , S = PE(x.getBoundingClientRect())
          , C = TE([...v, ...S]);
        l(C),
        f(!0)
    }
    , [f]);
    return g.useEffect( () => () => c(), [c]),
    g.useEffect( () => {
        if (a && d) {
            const w = E => y(E, d)
              , x = E => y(E, a);
            return a.addEventListener("pointerleave", w),
            d.addEventListener("pointerleave", x),
            () => {
                a.removeEventListener("pointerleave", w),
                d.removeEventListener("pointerleave", x)
            }
        }
    }
    , [a, d, y, c]),
    g.useEffect( () => {
        if (s) {
            const w = x => {
                const E = x.target
                  , h = {
                    x: x.clientX,
                    y: x.clientY
                }
                  , p = (a == null ? void 0 : a.contains(E)) || (d == null ? void 0 : d.contains(E))
                  , v = !NE(h, s);
                p ? c() : v && (c(),
                u())
            }
            ;
            return document.addEventListener("pointermove", w),
            () => document.removeEventListener("pointermove", w)
        }
    }
    , [a, d, s, u, c]),
    m.jsx(dv, {
        ...e,
        ref: i
    })
}
)
  , [EE,SE] = ol(uv, {
    isInside: !1
})
  , dv = g.forwardRef( (e, t) => {
    const {__scopeTooltip: n, children: r, "aria-label": o, onEscapeKeyDown: i, onPointerDownOutside: s, ...l} = e
      , a = il(Yr, n)
      , u = mc(n)
      , {onClose: d} = a;
    return g.useEffect( () => (document.addEventListener(af, d),
    () => document.removeEventListener(af, d)), [d]),
    g.useEffect( () => {
        if (a.trigger) {
            const f = c => {
                const y = c.target;
                y != null && y.contains(a.trigger) && d()
            }
            ;
            return window.addEventListener("scroll", f, {
                capture: !0
            }),
            () => window.removeEventListener("scroll", f, {
                capture: !0
            })
        }
    }
    , [a.trigger, d]),
    m.jsx(Dm, {
        asChild: !0,
        disableOutsidePointerEvents: !1,
        onEscapeKeyDown: i,
        onPointerDownOutside: s,
        onFocusOutside: f => f.preventDefault(),
        onDismiss: d,
        children: m.jsxs(cE, {
            "data-state": a.stateAttribute,
            ...u,
            ...l,
            ref: t,
            style: {
                ...l.style,
                "--radix-tooltip-content-transform-origin": "var(--radix-popper-transform-origin)",
                "--radix-tooltip-content-available-width": "var(--radix-popper-available-width)",
                "--radix-tooltip-content-available-height": "var(--radix-popper-available-height)",
                "--radix-tooltip-trigger-width": "var(--radix-popper-anchor-width)",
                "--radix-tooltip-trigger-height": "var(--radix-popper-anchor-height)"
            },
            children: [m.jsx(Yh, {
                children: r
            }), m.jsx(EE, {
                scope: n,
                isInside: !0,
                children: m.jsx(ix, {
                    id: a.contentId,
                    role: "tooltip",
                    children: o || r
                })
            })]
        })
    })
}
);
cv.displayName = Yr;
var fv = "TooltipArrow"
  , CE = g.forwardRef( (e, t) => {
    const {__scopeTooltip: n, ...r} = e
      , o = mc(n);
    return SE(fv, n).isInside ? null : m.jsx(dE, {
        ...o,
        ...r,
        ref: t
    })
}
);
CE.displayName = fv;
function bE(e, t) {
    const n = Math.abs(t.top - e.y)
      , r = Math.abs(t.bottom - e.y)
      , o = Math.abs(t.right - e.x)
      , i = Math.abs(t.left - e.x);
    switch (Math.min(n, r, o, i)) {
    case i:
        return "left";
    case o:
        return "right";
    case n:
        return "top";
    case r:
        return "bottom";
    default:
        throw new Error("unreachable")
    }
}
function kE(e, t, n=5) {
    const r = [];
    switch (t) {
    case "top":
        r.push({
            x: e.x - n,
            y: e.y + n
        }, {
            x: e.x + n,
            y: e.y + n
        });
        break;
    case "bottom":
        r.push({
            x: e.x - n,
            y: e.y - n
        }, {
            x: e.x + n,
            y: e.y - n
        });
        break;
    case "left":
        r.push({
            x: e.x + n,
            y: e.y - n
        }, {
            x: e.x + n,
            y: e.y + n
        });
        break;
    case "right":
        r.push({
            x: e.x - n,
            y: e.y - n
        }, {
            x: e.x - n,
            y: e.y + n
        });
        break
    }
    return r
}
function PE(e) {
    const {top: t, right: n, bottom: r, left: o} = e;
    return [{
        x: o,
        y: t
    }, {
        x: n,
        y: t
    }, {
        x: n,
        y: r
    }, {
        x: o,
        y: r
    }]
}
function NE(e, t) {
    const {x: n, y: r} = e;
    let o = !1;
    for (let i = 0, s = t.length - 1; i < t.length; s = i++) {
        const l = t[i].x
          , a = t[i].y
          , u = t[s].x
          , d = t[s].y;
        a > r != d > r && n < (u - l) * (r - a) / (d - a) + l && (o = !o)
    }
    return o
}
function TE(e) {
    const t = e.slice();
    return t.sort( (n, r) => n.x < r.x ? -1 : n.x > r.x ? 1 : n.y < r.y ? -1 : n.y > r.y ? 1 : 0),
    RE(t)
}
function RE(e) {
    if (e.length <= 1)
        return e.slice();
    const t = [];
    for (let r = 0; r < e.length; r++) {
        const o = e[r];
        for (; t.length >= 2; ) {
            const i = t[t.length - 1]
              , s = t[t.length - 2];
            if ((i.x - s.x) * (o.y - s.y) >= (i.y - s.y) * (o.x - s.x))
                t.pop();
            else
                break
        }
        t.push(o)
    }
    t.pop();
    const n = [];
    for (let r = e.length - 1; r >= 0; r--) {
        const o = e[r];
        for (; n.length >= 2; ) {
            const i = n[n.length - 1]
              , s = n[n.length - 2];
            if ((i.x - s.x) * (o.y - s.y) >= (i.y - s.y) * (o.x - s.x))
                n.pop();
            else
                break
        }
        n.push(o)
    }
    return n.pop(),
    t.length === 1 && n.length === 1 && t[0].x === n[0].x && t[0].y === n[0].y ? t : t.concat(n)
}
var AE = av
  , pv = cv;
const jE = AE
  , OE = g.forwardRef( ({className: e, sideOffset: t=4, ...n}, r) => m.jsx(pv, {
    ref: r,
    sideOffset: t,
    className: Ct("z-50 overflow-hidden rounded-md border bg-popover px-3 py-1.5 text-sm text-popover-foreground shadow-md animate-in fade-in-0 zoom-in-95 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2", e),
    ...n
}));
OE.displayName = pv.displayName;
var sl = class {
    constructor() {
        this.listeners = new Set,
        this.subscribe = this.subscribe.bind(this)
    }
    subscribe(e) {
        return this.listeners.add(e),
        this.onSubscribe(),
        () => {
            this.listeners.delete(e),
            this.onUnsubscribe()
        }
    }
    hasListeners() {
        return this.listeners.size > 0
    }
    onSubscribe() {}
    onUnsubscribe() {}
}
  , ll = typeof window > "u" || "Deno"in globalThis;
function ft() {}
function _E(e, t) {
    return typeof e == "function" ? e(t) : e
}
function LE(e) {
    return typeof e == "number" && e >= 0 && e !== 1 / 0
}
function ME(e, t) {
    return Math.max(e + (t || 0) - Date.now(), 0)
}
function uf(e, t) {
    return typeof e == "function" ? e(t) : e
}
function IE(e, t) {
    return typeof e == "function" ? e(t) : e
}
function cf(e, t) {
    const {type: n="all", exact: r, fetchStatus: o, predicate: i, queryKey: s, stale: l} = e;
    if (s) {
        if (r) {
            if (t.queryHash !== vc(s, t.options))
                return !1
        } else if (!ei(t.queryKey, s))
            return !1
    }
    if (n !== "all") {
        const a = t.isActive();
        if (n === "active" && !a || n === "inactive" && a)
            return !1
    }
    return !(typeof l == "boolean" && t.isStale() !== l || o && o !== t.state.fetchStatus || i && !i(t))
}
function df(e, t) {
    const {exact: n, status: r, predicate: o, mutationKey: i} = e;
    if (i) {
        if (!t.options.mutationKey)
            return !1;
        if (n) {
            if (Jo(t.options.mutationKey) !== Jo(i))
                return !1
        } else if (!ei(t.options.mutationKey, i))
            return !1
    }
    return !(r && t.state.status !== r || o && !o(t))
}
function vc(e, t) {
    return ((t == null ? void 0 : t.queryKeyHashFn) || Jo)(e)
}
function Jo(e) {
    return JSON.stringify(e, (t, n) => nu(n) ? Object.keys(n).sort().reduce( (r, o) => (r[o] = n[o],
    r), {}) : n)
}
function ei(e, t) {
    return e === t ? !0 : typeof e != typeof t ? !1 : e && t && typeof e == "object" && typeof t == "object" ? !Object.keys(t).some(n => !ei(e[n], t[n])) : !1
}
function hv(e, t) {
    if (e === t)
        return e;
    const n = ff(e) && ff(t);
    if (n || nu(e) && nu(t)) {
        const r = n ? e : Object.keys(e)
          , o = r.length
          , i = n ? t : Object.keys(t)
          , s = i.length
          , l = n ? [] : {};
        let a = 0;
        for (let u = 0; u < s; u++) {
            const d = n ? u : i[u];
            (!n && r.includes(d) || n) && e[d] === void 0 && t[d] === void 0 ? (l[d] = void 0,
            a++) : (l[d] = hv(e[d], t[d]),
            l[d] === e[d] && e[d] !== void 0 && a++)
        }
        return o === s && a === o ? e : l
    }
    return t
}
function ff(e) {
    return Array.isArray(e) && e.length === Object.keys(e).length
}
function nu(e) {
    if (!pf(e))
        return !1;
    const t = e.constructor;
    if (t === void 0)
        return !0;
    const n = t.prototype;
    return !(!pf(n) || !n.hasOwnProperty("isPrototypeOf") || Object.getPrototypeOf(e) !== Object.prototype)
}
function pf(e) {
    return Object.prototype.toString.call(e) === "[object Object]"
}
function DE(e) {
    return new Promise(t => {
        setTimeout(t, e)
    }
    )
}
function zE(e, t, n) {
    return typeof n.structuralSharing == "function" ? n.structuralSharing(e, t) : n.structuralSharing !== !1 ? hv(e, t) : t
}
function FE(e, t, n=0) {
    const r = [...e, t];
    return n && r.length > n ? r.slice(1) : r
}
function $E(e, t, n=0) {
    const r = [t, ...e];
    return n && r.length > n ? r.slice(0, -1) : r
}
var mv = Symbol();
function vv(e, t) {
    return !e.queryFn && (t != null && t.initialPromise) ? () => t.initialPromise : !e.queryFn || e.queryFn === mv ? () => Promise.reject(new Error(`Missing queryFn: '${e.queryHash}'`)) : e.queryFn
}
var Hn, dn, Or, bf, UE = (bf = class extends sl {
    constructor() {
        super();
        q(this, Hn);
        q(this, dn);
        q(this, Or);
        V(this, Or, t => {
            if (!ll && window.addEventListener) {
                const n = () => t();
                return window.addEventListener("visibilitychange", n, !1),
                () => {
                    window.removeEventListener("visibilitychange", n)
                }
            }
        }
        )
    }
    onSubscribe() {
        T(this, dn) || this.setEventListener(T(this, Or))
    }
    onUnsubscribe() {
        var t;
        this.hasListeners() || ((t = T(this, dn)) == null || t.call(this),
        V(this, dn, void 0))
    }
    setEventListener(t) {
        var n;
        V(this, Or, t),
        (n = T(this, dn)) == null || n.call(this),
        V(this, dn, t(r => {
            typeof r == "boolean" ? this.setFocused(r) : this.onFocus()
        }
        ))
    }
    setFocused(t) {
        T(this, Hn) !== t && (V(this, Hn, t),
        this.onFocus())
    }
    onFocus() {
        const t = this.isFocused();
        this.listeners.forEach(n => {
            n(t)
        }
        )
    }
    isFocused() {
        var t;
        return typeof T(this, Hn) == "boolean" ? T(this, Hn) : ((t = globalThis.document) == null ? void 0 : t.visibilityState) !== "hidden"
    }
}
,
Hn = new WeakMap,
dn = new WeakMap,
Or = new WeakMap,
bf), gv = new UE, _r, fn, Lr, kf, BE = (kf = class extends sl {
    constructor() {
        super();
        q(this, _r, !0);
        q(this, fn);
        q(this, Lr);
        V(this, Lr, t => {
            if (!ll && window.addEventListener) {
                const n = () => t(!0)
                  , r = () => t(!1);
                return window.addEventListener("online", n, !1),
                window.addEventListener("offline", r, !1),
                () => {
                    window.removeEventListener("online", n),
                    window.removeEventListener("offline", r)
                }
            }
        }
        )
    }
    onSubscribe() {
        T(this, fn) || this.setEventListener(T(this, Lr))
    }
    onUnsubscribe() {
        var t;
        this.hasListeners() || ((t = T(this, fn)) == null || t.call(this),
        V(this, fn, void 0))
    }
    setEventListener(t) {
        var n;
        V(this, Lr, t),
        (n = T(this, fn)) == null || n.call(this),
        V(this, fn, t(this.setOnline.bind(this)))
    }
    setOnline(t) {
        T(this, _r) !== t && (V(this, _r, t),
        this.listeners.forEach(r => {
            r(t)
        }
        ))
    }
    isOnline() {
        return T(this, _r)
    }
}
,
_r = new WeakMap,
fn = new WeakMap,
Lr = new WeakMap,
kf), js = new BE;
function WE(e) {
    return Math.min(1e3 * 2 ** e, 3e4)
}
function yv(e) {
    return (e ?? "online") === "online" ? js.isOnline() : !0
}
var xv = class extends Error {
    constructor(e) {
        super("CancelledError"),
        this.revert = e == null ? void 0 : e.revert,
        this.silent = e == null ? void 0 : e.silent
    }
}
;
function Yl(e) {
    return e instanceof xv
}
function wv(e) {
    let t = !1, n = 0, r = !1, o, i, s;
    const l = new Promise( (h, p) => {
        i = h,
        s = p
    }
    )
      , a = h => {
        var p;
        r || (w(new xv(h)),
        (p = e.abort) == null || p.call(e))
    }
      , u = () => {
        t = !0
    }
      , d = () => {
        t = !1
    }
      , f = () => gv.isFocused() && (e.networkMode === "always" || js.isOnline()) && e.canRun()
      , c = () => yv(e.networkMode) && e.canRun()
      , y = h => {
        var p;
        r || (r = !0,
        (p = e.onSuccess) == null || p.call(e, h),
        o == null || o(),
        i(h))
    }
      , w = h => {
        var p;
        r || (r = !0,
        (p = e.onError) == null || p.call(e, h),
        o == null || o(),
        s(h))
    }
      , x = () => new Promise(h => {
        var p;
        o = v => {
            (r || f()) && h(v)
        }
        ,
        (p = e.onPause) == null || p.call(e)
    }
    ).then( () => {
        var h;
        o = void 0,
        r || (h = e.onContinue) == null || h.call(e)
    }
    )
      , E = () => {
        if (r)
            return;
        let h;
        const p = n === 0 ? e.initialPromise : void 0;
        try {
            h = p ?? e.fn()
        } catch (v) {
            h = Promise.reject(v)
        }
        Promise.resolve(h).then(y).catch(v => {
            var N;
            if (r)
                return;
            const S = e.retry ?? (ll ? 0 : 3)
              , C = e.retryDelay ?? WE
              , P = typeof C == "function" ? C(n, v) : C
              , b = S === !0 || typeof S == "number" && n < S || typeof S == "function" && S(n, v);
            if (t || !b) {
                w(v);
                return
            }
            n++,
            (N = e.onFail) == null || N.call(e, n, v),
            DE(P).then( () => f() ? void 0 : x()).then( () => {
                t ? w(v) : E()
            }
            )
        }
        )
    }
    ;
    return {
        promise: l,
        cancel: a,
        continue: () => (o == null || o(),
        l),
        cancelRetry: u,
        continueRetry: d,
        canStart: c,
        start: () => (c() ? E() : x().then(E),
        l)
    }
}
function VE() {
    let e = []
      , t = 0
      , n = l => {
        l()
    }
      , r = l => {
        l()
    }
      , o = l => setTimeout(l, 0);
    const i = l => {
        t ? e.push(l) : o( () => {
            n(l)
        }
        )
    }
      , s = () => {
        const l = e;
        e = [],
        l.length && o( () => {
            r( () => {
                l.forEach(a => {
                    n(a)
                }
                )
            }
            )
        }
        )
    }
    ;
    return {
        batch: l => {
            let a;
            t++;
            try {
                a = l()
            } finally {
                t--,
                t || s()
            }
            return a
        }
        ,
        batchCalls: l => (...a) => {
            i( () => {
                l(...a)
            }
            )
        }
        ,
        schedule: i,
        setNotifyFunction: l => {
            n = l
        }
        ,
        setBatchNotifyFunction: l => {
            r = l
        }
        ,
        setScheduler: l => {
            o = l
        }
    }
}
var Me = VE(), Qn, Pf, Ev = (Pf = class {
    constructor() {
        q(this, Qn)
    }
    destroy() {
        this.clearGcTimeout()
    }
    scheduleGc() {
        this.clearGcTimeout(),
        LE(this.gcTime) && V(this, Qn, setTimeout( () => {
            this.optionalRemove()
        }
        , this.gcTime))
    }
    updateGcTime(e) {
        this.gcTime = Math.max(this.gcTime || 0, e ?? (ll ? 1 / 0 : 5 * 60 * 1e3))
    }
    clearGcTimeout() {
        T(this, Qn) && (clearTimeout(T(this, Qn)),
        V(this, Qn, void 0))
    }
}
,
Qn = new WeakMap,
Pf), Mr, Ir, tt, Re, ri, Kn, pt, Dt, Nf, HE = (Nf = class extends Ev {
    constructor(t) {
        super();
        q(this, pt);
        q(this, Mr);
        q(this, Ir);
        q(this, tt);
        q(this, Re);
        q(this, ri);
        q(this, Kn);
        V(this, Kn, !1),
        V(this, ri, t.defaultOptions),
        this.setOptions(t.options),
        this.observers = [],
        V(this, tt, t.cache),
        this.queryKey = t.queryKey,
        this.queryHash = t.queryHash,
        V(this, Mr, KE(this.options)),
        this.state = t.state ?? T(this, Mr),
        this.scheduleGc()
    }
    get meta() {
        return this.options.meta
    }
    get promise() {
        var t;
        return (t = T(this, Re)) == null ? void 0 : t.promise
    }
    setOptions(t) {
        this.options = {
            ...T(this, ri),
            ...t
        },
        this.updateGcTime(this.options.gcTime)
    }
    optionalRemove() {
        !this.observers.length && this.state.fetchStatus === "idle" && T(this, tt).remove(this)
    }
    setData(t, n) {
        const r = zE(this.state.data, t, this.options);
        return Pe(this, pt, Dt).call(this, {
            data: r,
            type: "success",
            dataUpdatedAt: n == null ? void 0 : n.updatedAt,
            manual: n == null ? void 0 : n.manual
        }),
        r
    }
    setState(t, n) {
        Pe(this, pt, Dt).call(this, {
            type: "setState",
            state: t,
            setStateOptions: n
        })
    }
    cancel(t) {
        var r, o;
        const n = (r = T(this, Re)) == null ? void 0 : r.promise;
        return (o = T(this, Re)) == null || o.cancel(t),
        n ? n.then(ft).catch(ft) : Promise.resolve()
    }
    destroy() {
        super.destroy(),
        this.cancel({
            silent: !0
        })
    }
    reset() {
        this.destroy(),
        this.setState(T(this, Mr))
    }
    isActive() {
        return this.observers.some(t => IE(t.options.enabled, this) !== !1)
    }
    isDisabled() {
        return this.getObserversCount() > 0 && !this.isActive()
    }
    isStale() {
        return this.state.isInvalidated ? !0 : this.getObserversCount() > 0 ? this.observers.some(t => t.getCurrentResult().isStale) : this.state.data === void 0
    }
    isStaleByTime(t=0) {
        return this.state.isInvalidated || this.state.data === void 0 || !ME(this.state.dataUpdatedAt, t)
    }
    onFocus() {
        var n;
        const t = this.observers.find(r => r.shouldFetchOnWindowFocus());
        t == null || t.refetch({
            cancelRefetch: !1
        }),
        (n = T(this, Re)) == null || n.continue()
    }
    onOnline() {
        var n;
        const t = this.observers.find(r => r.shouldFetchOnReconnect());
        t == null || t.refetch({
            cancelRefetch: !1
        }),
        (n = T(this, Re)) == null || n.continue()
    }
    addObserver(t) {
        this.observers.includes(t) || (this.observers.push(t),
        this.clearGcTimeout(),
        T(this, tt).notify({
            type: "observerAdded",
            query: this,
            observer: t
        }))
    }
    removeObserver(t) {
        this.observers.includes(t) && (this.observers = this.observers.filter(n => n !== t),
        this.observers.length || (T(this, Re) && (T(this, Kn) ? T(this, Re).cancel({
            revert: !0
        }) : T(this, Re).cancelRetry()),
        this.scheduleGc()),
        T(this, tt).notify({
            type: "observerRemoved",
            query: this,
            observer: t
        }))
    }
    getObserversCount() {
        return this.observers.length
    }
    invalidate() {
        this.state.isInvalidated || Pe(this, pt, Dt).call(this, {
            type: "invalidate"
        })
    }
    fetch(t, n) {
        var a, u, d;
        if (this.state.fetchStatus !== "idle") {
            if (this.state.data !== void 0 && (n != null && n.cancelRefetch))
                this.cancel({
                    silent: !0
                });
            else if (T(this, Re))
                return T(this, Re).continueRetry(),
                T(this, Re).promise
        }
        if (t && this.setOptions(t),
        !this.options.queryFn) {
            const f = this.observers.find(c => c.options.queryFn);
            f && this.setOptions(f.options)
        }
        const r = new AbortController
          , o = f => {
            Object.defineProperty(f, "signal", {
                enumerable: !0,
                get: () => (V(this, Kn, !0),
                r.signal)
            })
        }
          , i = () => {
            const f = vv(this.options, n)
              , c = {
                queryKey: this.queryKey,
                meta: this.meta
            };
            return o(c),
            V(this, Kn, !1),
            this.options.persister ? this.options.persister(f, c, this) : f(c)
        }
          , s = {
            fetchOptions: n,
            options: this.options,
            queryKey: this.queryKey,
            state: this.state,
            fetchFn: i
        };
        o(s),
        (a = this.options.behavior) == null || a.onFetch(s, this),
        V(this, Ir, this.state),
        (this.state.fetchStatus === "idle" || this.state.fetchMeta !== ((u = s.fetchOptions) == null ? void 0 : u.meta)) && Pe(this, pt, Dt).call(this, {
            type: "fetch",
            meta: (d = s.fetchOptions) == null ? void 0 : d.meta
        });
        const l = f => {
            var c, y, w, x;
            Yl(f) && f.silent || Pe(this, pt, Dt).call(this, {
                type: "error",
                error: f
            }),
            Yl(f) || ((y = (c = T(this, tt).config).onError) == null || y.call(c, f, this),
            (x = (w = T(this, tt).config).onSettled) == null || x.call(w, this.state.data, f, this)),
            this.isFetchingOptimistic || this.scheduleGc(),
            this.isFetchingOptimistic = !1
        }
        ;
        return V(this, Re, wv({
            initialPromise: n == null ? void 0 : n.initialPromise,
            fn: s.fetchFn,
            abort: r.abort.bind(r),
            onSuccess: f => {
                var c, y, w, x;
                if (f === void 0) {
                    l(new Error(`${this.queryHash} data is undefined`));
                    return
                }
                try {
                    this.setData(f)
                } catch (E) {
                    l(E);
                    return
                }
                (y = (c = T(this, tt).config).onSuccess) == null || y.call(c, f, this),
                (x = (w = T(this, tt).config).onSettled) == null || x.call(w, f, this.state.error, this),
                this.isFetchingOptimistic || this.scheduleGc(),
                this.isFetchingOptimistic = !1
            }
            ,
            onError: l,
            onFail: (f, c) => {
                Pe(this, pt, Dt).call(this, {
                    type: "failed",
                    failureCount: f,
                    error: c
                })
            }
            ,
            onPause: () => {
                Pe(this, pt, Dt).call(this, {
                    type: "pause"
                })
            }
            ,
            onContinue: () => {
                Pe(this, pt, Dt).call(this, {
                    type: "continue"
                })
            }
            ,
            retry: s.options.retry,
            retryDelay: s.options.retryDelay,
            networkMode: s.options.networkMode,
            canRun: () => !0
        })),
        T(this, Re).start()
    }
}
,
Mr = new WeakMap,
Ir = new WeakMap,
tt = new WeakMap,
Re = new WeakMap,
ri = new WeakMap,
Kn = new WeakMap,
pt = new WeakSet,
Dt = function(t) {
    const n = r => {
        switch (t.type) {
        case "failed":
            return {
                ...r,
                fetchFailureCount: t.failureCount,
                fetchFailureReason: t.error
            };
        case "pause":
            return {
                ...r,
                fetchStatus: "paused"
            };
        case "continue":
            return {
                ...r,
                fetchStatus: "fetching"
            };
        case "fetch":
            return {
                ...r,
                ...QE(r.data, this.options),
                fetchMeta: t.meta ?? null
            };
        case "success":
            return {
                ...r,
                data: t.data,
                dataUpdateCount: r.dataUpdateCount + 1,
                dataUpdatedAt: t.dataUpdatedAt ?? Date.now(),
                error: null,
                isInvalidated: !1,
                status: "success",
                ...!t.manual && {
                    fetchStatus: "idle",
                    fetchFailureCount: 0,
                    fetchFailureReason: null
                }
            };
        case "error":
            const o = t.error;
            return Yl(o) && o.revert && T(this, Ir) ? {
                ...T(this, Ir),
                fetchStatus: "idle"
            } : {
                ...r,
                error: o,
                errorUpdateCount: r.errorUpdateCount + 1,
                errorUpdatedAt: Date.now(),
                fetchFailureCount: r.fetchFailureCount + 1,
                fetchFailureReason: o,
                fetchStatus: "idle",
                status: "error"
            };
        case "invalidate":
            return {
                ...r,
                isInvalidated: !0
            };
        case "setState":
            return {
                ...r,
                ...t.state
            }
        }
    }
    ;
    this.state = n(this.state),
    Me.batch( () => {
        this.observers.forEach(r => {
            r.onQueryUpdate()
        }
        ),
        T(this, tt).notify({
            query: this,
            type: "updated",
            action: t
        })
    }
    )
}
,
Nf);
function QE(e, t) {
    return {
        fetchFailureCount: 0,
        fetchFailureReason: null,
        fetchStatus: yv(t.networkMode) ? "fetching" : "paused",
        ...e === void 0 && {
            error: null,
            status: "pending"
        }
    }
}
function KE(e) {
    const t = typeof e.initialData == "function" ? e.initialData() : e.initialData
      , n = t !== void 0
      , r = n ? typeof e.initialDataUpdatedAt == "function" ? e.initialDataUpdatedAt() : e.initialDataUpdatedAt : 0;
    return {
        data: t,
        dataUpdateCount: 0,
        dataUpdatedAt: n ? r ?? Date.now() : 0,
        error: null,
        errorUpdateCount: 0,
        errorUpdatedAt: 0,
        fetchFailureCount: 0,
        fetchFailureReason: null,
        fetchMeta: null,
        isInvalidated: !1,
        status: n ? "success" : "pending",
        fetchStatus: "idle"
    }
}
var Tt, Tf, GE = (Tf = class extends sl {
    constructor(t={}) {
        super();
        q(this, Tt);
        this.config = t,
        V(this, Tt, new Map)
    }
    build(t, n, r) {
        const o = n.queryKey
          , i = n.queryHash ?? vc(o, n);
        let s = this.get(i);
        return s || (s = new HE({
            cache: this,
            queryKey: o,
            queryHash: i,
            options: t.defaultQueryOptions(n),
            state: r,
            defaultOptions: t.getQueryDefaults(o)
        }),
        this.add(s)),
        s
    }
    add(t) {
        T(this, Tt).has(t.queryHash) || (T(this, Tt).set(t.queryHash, t),
        this.notify({
            type: "added",
            query: t
        }))
    }
    remove(t) {
        const n = T(this, Tt).get(t.queryHash);
        n && (t.destroy(),
        n === t && T(this, Tt).delete(t.queryHash),
        this.notify({
            type: "removed",
            query: t
        }))
    }
    clear() {
        Me.batch( () => {
            this.getAll().forEach(t => {
                this.remove(t)
            }
            )
        }
        )
    }
    get(t) {
        return T(this, Tt).get(t)
    }
    getAll() {
        return [...T(this, Tt).values()]
    }
    find(t) {
        const n = {
            exact: !0,
            ...t
        };
        return this.getAll().find(r => cf(n, r))
    }
    findAll(t={}) {
        const n = this.getAll();
        return Object.keys(t).length > 0 ? n.filter(r => cf(t, r)) : n
    }
    notify(t) {
        Me.batch( () => {
            this.listeners.forEach(n => {
                n(t)
            }
            )
        }
        )
    }
    onFocus() {
        Me.batch( () => {
            this.getAll().forEach(t => {
                t.onFocus()
            }
            )
        }
        )
    }
    onOnline() {
        Me.batch( () => {
            this.getAll().forEach(t => {
                t.onOnline()
            }
            )
        }
        )
    }
}
,
Tt = new WeakMap,
Tf), Rt, _e, Gn, At, sn, Rf, YE = (Rf = class extends Ev {
    constructor(t) {
        super();
        q(this, At);
        q(this, Rt);
        q(this, _e);
        q(this, Gn);
        this.mutationId = t.mutationId,
        V(this, _e, t.mutationCache),
        V(this, Rt, []),
        this.state = t.state || XE(),
        this.setOptions(t.options),
        this.scheduleGc()
    }
    setOptions(t) {
        this.options = t,
        this.updateGcTime(this.options.gcTime)
    }
    get meta() {
        return this.options.meta
    }
    addObserver(t) {
        T(this, Rt).includes(t) || (T(this, Rt).push(t),
        this.clearGcTimeout(),
        T(this, _e).notify({
            type: "observerAdded",
            mutation: this,
            observer: t
        }))
    }
    removeObserver(t) {
        V(this, Rt, T(this, Rt).filter(n => n !== t)),
        this.scheduleGc(),
        T(this, _e).notify({
            type: "observerRemoved",
            mutation: this,
            observer: t
        })
    }
    optionalRemove() {
        T(this, Rt).length || (this.state.status === "pending" ? this.scheduleGc() : T(this, _e).remove(this))
    }
    continue() {
        var t;
        return ((t = T(this, Gn)) == null ? void 0 : t.continue()) ?? this.execute(this.state.variables)
    }
    async execute(t) {
        var o, i, s, l, a, u, d, f, c, y, w, x, E, h, p, v, S, C, P, b;
        V(this, Gn, wv({
            fn: () => this.options.mutationFn ? this.options.mutationFn(t) : Promise.reject(new Error("No mutationFn found")),
            onFail: (N, _) => {
                Pe(this, At, sn).call(this, {
                    type: "failed",
                    failureCount: N,
                    error: _
                })
            }
            ,
            onPause: () => {
                Pe(this, At, sn).call(this, {
                    type: "pause"
                })
            }
            ,
            onContinue: () => {
                Pe(this, At, sn).call(this, {
                    type: "continue"
                })
            }
            ,
            retry: this.options.retry ?? 0,
            retryDelay: this.options.retryDelay,
            networkMode: this.options.networkMode,
            canRun: () => T(this, _e).canRun(this)
        }));
        const n = this.state.status === "pending"
          , r = !T(this, Gn).canStart();
        try {
            if (!n) {
                Pe(this, At, sn).call(this, {
                    type: "pending",
                    variables: t,
                    isPaused: r
                }),
                await ((i = (o = T(this, _e).config).onMutate) == null ? void 0 : i.call(o, t, this));
                const _ = await ((l = (s = this.options).onMutate) == null ? void 0 : l.call(s, t));
                _ !== this.state.context && Pe(this, At, sn).call(this, {
                    type: "pending",
                    context: _,
                    variables: t,
                    isPaused: r
                })
            }
            const N = await T(this, Gn).start();
            return await ((u = (a = T(this, _e).config).onSuccess) == null ? void 0 : u.call(a, N, t, this.state.context, this)),
            await ((f = (d = this.options).onSuccess) == null ? void 0 : f.call(d, N, t, this.state.context)),
            await ((y = (c = T(this, _e).config).onSettled) == null ? void 0 : y.call(c, N, null, this.state.variables, this.state.context, this)),
            await ((x = (w = this.options).onSettled) == null ? void 0 : x.call(w, N, null, t, this.state.context)),
            Pe(this, At, sn).call(this, {
                type: "success",
                data: N
            }),
            N
        } catch (N) {
            try {
                throw await ((h = (E = T(this, _e).config).onError) == null ? void 0 : h.call(E, N, t, this.state.context, this)),
                await ((v = (p = this.options).onError) == null ? void 0 : v.call(p, N, t, this.state.context)),
                await ((C = (S = T(this, _e).config).onSettled) == null ? void 0 : C.call(S, void 0, N, this.state.variables, this.state.context, this)),
                await ((b = (P = this.options).onSettled) == null ? void 0 : b.call(P, void 0, N, t, this.state.context)),
                N
            } finally {
                Pe(this, At, sn).call(this, {
                    type: "error",
                    error: N
                })
            }
        } finally {
            T(this, _e).runNext(this)
        }
    }
}
,
Rt = new WeakMap,
_e = new WeakMap,
Gn = new WeakMap,
At = new WeakSet,
sn = function(t) {
    const n = r => {
        switch (t.type) {
        case "failed":
            return {
                ...r,
                failureCount: t.failureCount,
                failureReason: t.error
            };
        case "pause":
            return {
                ...r,
                isPaused: !0
            };
        case "continue":
            return {
                ...r,
                isPaused: !1
            };
        case "pending":
            return {
                ...r,
                context: t.context,
                data: void 0,
                failureCount: 0,
                failureReason: null,
                error: null,
                isPaused: t.isPaused,
                status: "pending",
                variables: t.variables,
                submittedAt: Date.now()
            };
        case "success":
            return {
                ...r,
                data: t.data,
                failureCount: 0,
                failureReason: null,
                error: null,
                status: "success",
                isPaused: !1
            };
        case "error":
            return {
                ...r,
                data: void 0,
                error: t.error,
                failureCount: r.failureCount + 1,
                failureReason: t.error,
                isPaused: !1,
                status: "error"
            }
        }
    }
    ;
    this.state = n(this.state),
    Me.batch( () => {
        T(this, Rt).forEach(r => {
            r.onMutationUpdate(t)
        }
        ),
        T(this, _e).notify({
            mutation: this,
            type: "updated",
            action: t
        })
    }
    )
}
,
Rf);
function XE() {
    return {
        context: void 0,
        data: void 0,
        error: null,
        failureCount: 0,
        failureReason: null,
        isPaused: !1,
        status: "idle",
        variables: void 0,
        submittedAt: 0
    }
}
var He, oi, Af, qE = (Af = class extends sl {
    constructor(t={}) {
        super();
        q(this, He);
        q(this, oi);
        this.config = t,
        V(this, He, new Map),
        V(this, oi, Date.now())
    }
    build(t, n, r) {
        const o = new YE({
            mutationCache: this,
            mutationId: ++yi(this, oi)._,
            options: t.defaultMutationOptions(n),
            state: r
        });
        return this.add(o),
        o
    }
    add(t) {
        const n = Ui(t)
          , r = T(this, He).get(n) ?? [];
        r.push(t),
        T(this, He).set(n, r),
        this.notify({
            type: "added",
            mutation: t
        })
    }
    remove(t) {
        var r;
        const n = Ui(t);
        if (T(this, He).has(n)) {
            const o = (r = T(this, He).get(n)) == null ? void 0 : r.filter(i => i !== t);
            o && (o.length === 0 ? T(this, He).delete(n) : T(this, He).set(n, o))
        }
        this.notify({
            type: "removed",
            mutation: t
        })
    }
    canRun(t) {
        var r;
        const n = (r = T(this, He).get(Ui(t))) == null ? void 0 : r.find(o => o.state.status === "pending");
        return !n || n === t
    }
    runNext(t) {
        var r;
        const n = (r = T(this, He).get(Ui(t))) == null ? void 0 : r.find(o => o !== t && o.state.isPaused);
        return (n == null ? void 0 : n.continue()) ?? Promise.resolve()
    }
    clear() {
        Me.batch( () => {
            this.getAll().forEach(t => {
                this.remove(t)
            }
            )
        }
        )
    }
    getAll() {
        return [...T(this, He).values()].flat()
    }
    find(t) {
        const n = {
            exact: !0,
            ...t
        };
        return this.getAll().find(r => df(n, r))
    }
    findAll(t={}) {
        return this.getAll().filter(n => df(t, n))
    }
    notify(t) {
        Me.batch( () => {
            this.listeners.forEach(n => {
                n(t)
            }
            )
        }
        )
    }
    resumePausedMutations() {
        const t = this.getAll().filter(n => n.state.isPaused);
        return Me.batch( () => Promise.all(t.map(n => n.continue().catch(ft))))
    }
}
,
He = new WeakMap,
oi = new WeakMap,
Af);
function Ui(e) {
    var t;
    return ((t = e.options.scope) == null ? void 0 : t.id) ?? String(e.mutationId)
}
function hf(e) {
    return {
        onFetch: (t, n) => {
            var d, f, c, y, w;
            const r = t.options
              , o = (c = (f = (d = t.fetchOptions) == null ? void 0 : d.meta) == null ? void 0 : f.fetchMore) == null ? void 0 : c.direction
              , i = ((y = t.state.data) == null ? void 0 : y.pages) || []
              , s = ((w = t.state.data) == null ? void 0 : w.pageParams) || [];
            let l = {
                pages: [],
                pageParams: []
            }
              , a = 0;
            const u = async () => {
                let x = !1;
                const E = v => {
                    Object.defineProperty(v, "signal", {
                        enumerable: !0,
                        get: () => (t.signal.aborted ? x = !0 : t.signal.addEventListener("abort", () => {
                            x = !0
                        }
                        ),
                        t.signal)
                    })
                }
                  , h = vv(t.options, t.fetchOptions)
                  , p = async (v, S, C) => {
                    if (x)
                        return Promise.reject();
                    if (S == null && v.pages.length)
                        return Promise.resolve(v);
                    const P = {
                        queryKey: t.queryKey,
                        pageParam: S,
                        direction: C ? "backward" : "forward",
                        meta: t.options.meta
                    };
                    E(P);
                    const b = await h(P)
                      , {maxPages: N} = t.options
                      , _ = C ? $E : FE;
                    return {
                        pages: _(v.pages, b, N),
                        pageParams: _(v.pageParams, S, N)
                    }
                }
                ;
                if (o && i.length) {
                    const v = o === "backward"
                      , S = v ? ZE : mf
                      , C = {
                        pages: i,
                        pageParams: s
                    }
                      , P = S(r, C);
                    l = await p(C, P, v)
                } else {
                    const v = e ?? i.length;
                    do {
                        const S = a === 0 ? s[0] ?? r.initialPageParam : mf(r, l);
                        if (a > 0 && S == null)
                            break;
                        l = await p(l, S),
                        a++
                    } while (a < v)
                }
                return l
            }
            ;
            t.options.persister ? t.fetchFn = () => {
                var x, E;
                return (E = (x = t.options).persister) == null ? void 0 : E.call(x, u, {
                    queryKey: t.queryKey,
                    meta: t.options.meta,
                    signal: t.signal
                }, n)
            }
            : t.fetchFn = u
        }
    }
}
function mf(e, {pages: t, pageParams: n}) {
    const r = t.length - 1;
    return t.length > 0 ? e.getNextPageParam(t[r], t, n[r], n) : void 0
}
function ZE(e, {pages: t, pageParams: n}) {
    var r;
    return t.length > 0 ? (r = e.getPreviousPageParam) == null ? void 0 : r.call(e, t[0], t, n[0], n) : void 0
}
var fe, pn, hn, Dr, zr, mn, Fr, $r, jf, JE = (jf = class {
    constructor(e={}) {
        q(this, fe);
        q(this, pn);
        q(this, hn);
        q(this, Dr);
        q(this, zr);
        q(this, mn);
        q(this, Fr);
        q(this, $r);
        V(this, fe, e.queryCache || new GE),
        V(this, pn, e.mutationCache || new qE),
        V(this, hn, e.defaultOptions || {}),
        V(this, Dr, new Map),
        V(this, zr, new Map),
        V(this, mn, 0)
    }
    mount() {
        yi(this, mn)._++,
        T(this, mn) === 1 && (V(this, Fr, gv.subscribe(async e => {
            e && (await this.resumePausedMutations(),
            T(this, fe).onFocus())
        }
        )),
        V(this, $r, js.subscribe(async e => {
            e && (await this.resumePausedMutations(),
            T(this, fe).onOnline())
        }
        )))
    }
    unmount() {
        var e, t;
        yi(this, mn)._--,
        T(this, mn) === 0 && ((e = T(this, Fr)) == null || e.call(this),
        V(this, Fr, void 0),
        (t = T(this, $r)) == null || t.call(this),
        V(this, $r, void 0))
    }
    isFetching(e) {
        return T(this, fe).findAll({
            ...e,
            fetchStatus: "fetching"
        }).length
    }
    isMutating(e) {
        return T(this, pn).findAll({
            ...e,
            status: "pending"
        }).length
    }
    getQueryData(e) {
        var n;
        const t = this.defaultQueryOptions({
            queryKey: e
        });
        return (n = T(this, fe).get(t.queryHash)) == null ? void 0 : n.state.data
    }
    ensureQueryData(e) {
        const t = this.getQueryData(e.queryKey);
        if (t === void 0)
            return this.fetchQuery(e);
        {
            const n = this.defaultQueryOptions(e)
              , r = T(this, fe).build(this, n);
            return e.revalidateIfStale && r.isStaleByTime(uf(n.staleTime, r)) && this.prefetchQuery(n),
            Promise.resolve(t)
        }
    }
    getQueriesData(e) {
        return T(this, fe).findAll(e).map( ({queryKey: t, state: n}) => {
            const r = n.data;
            return [t, r]
        }
        )
    }
    setQueryData(e, t, n) {
        const r = this.defaultQueryOptions({
            queryKey: e
        })
          , o = T(this, fe).get(r.queryHash)
          , i = o == null ? void 0 : o.state.data
          , s = _E(t, i);
        if (s !== void 0)
            return T(this, fe).build(this, r).setData(s, {
                ...n,
                manual: !0
            })
    }
    setQueriesData(e, t, n) {
        return Me.batch( () => T(this, fe).findAll(e).map( ({queryKey: r}) => [r, this.setQueryData(r, t, n)]))
    }
    getQueryState(e) {
        var n;
        const t = this.defaultQueryOptions({
            queryKey: e
        });
        return (n = T(this, fe).get(t.queryHash)) == null ? void 0 : n.state
    }
    removeQueries(e) {
        const t = T(this, fe);
        Me.batch( () => {
            t.findAll(e).forEach(n => {
                t.remove(n)
            }
            )
        }
        )
    }
    resetQueries(e, t) {
        const n = T(this, fe)
          , r = {
            type: "active",
            ...e
        };
        return Me.batch( () => (n.findAll(e).forEach(o => {
            o.reset()
        }
        ),
        this.refetchQueries(r, t)))
    }
    cancelQueries(e={}, t={}) {
        const n = {
            revert: !0,
            ...t
        }
          , r = Me.batch( () => T(this, fe).findAll(e).map(o => o.cancel(n)));
        return Promise.all(r).then(ft).catch(ft)
    }
    invalidateQueries(e={}, t={}) {
        return Me.batch( () => {
            if (T(this, fe).findAll(e).forEach(r => {
                r.invalidate()
            }
            ),
            e.refetchType === "none")
                return Promise.resolve();
            const n = {
                ...e,
                type: e.refetchType ?? e.type ?? "active"
            };
            return this.refetchQueries(n, t)
        }
        )
    }
    refetchQueries(e={}, t) {
        const n = {
            ...t,
            cancelRefetch: (t == null ? void 0 : t.cancelRefetch) ?? !0
        }
          , r = Me.batch( () => T(this, fe).findAll(e).filter(o => !o.isDisabled()).map(o => {
            let i = o.fetch(void 0, n);
            return n.throwOnError || (i = i.catch(ft)),
            o.state.fetchStatus === "paused" ? Promise.resolve() : i
        }
        ));
        return Promise.all(r).then(ft)
    }
    fetchQuery(e) {
        const t = this.defaultQueryOptions(e);
        t.retry === void 0 && (t.retry = !1);
        const n = T(this, fe).build(this, t);
        return n.isStaleByTime(uf(t.staleTime, n)) ? n.fetch(t) : Promise.resolve(n.state.data)
    }
    prefetchQuery(e) {
        return this.fetchQuery(e).then(ft).catch(ft)
    }
    fetchInfiniteQuery(e) {
        return e.behavior = hf(e.pages),
        this.fetchQuery(e)
    }
    prefetchInfiniteQuery(e) {
        return this.fetchInfiniteQuery(e).then(ft).catch(ft)
    }
    ensureInfiniteQueryData(e) {
        return e.behavior = hf(e.pages),
        this.ensureQueryData(e)
    }
    resumePausedMutations() {
        return js.isOnline() ? T(this, pn).resumePausedMutations() : Promise.resolve()
    }
    getQueryCache() {
        return T(this, fe)
    }
    getMutationCache() {
        return T(this, pn)
    }
    getDefaultOptions() {
        return T(this, hn)
    }
    setDefaultOptions(e) {
        V(this, hn, e)
    }
    setQueryDefaults(e, t) {
        T(this, Dr).set(Jo(e), {
            queryKey: e,
            defaultOptions: t
        })
    }
    getQueryDefaults(e) {
        const t = [...T(this, Dr).values()];
        let n = {};
        return t.forEach(r => {
            ei(e, r.queryKey) && (n = {
                ...n,
                ...r.defaultOptions
            })
        }
        ),
        n
    }
    setMutationDefaults(e, t) {
        T(this, zr).set(Jo(e), {
            mutationKey: e,
            defaultOptions: t
        })
    }
    getMutationDefaults(e) {
        const t = [...T(this, zr).values()];
        let n = {};
        return t.forEach(r => {
            ei(e, r.mutationKey) && (n = {
                ...n,
                ...r.defaultOptions
            })
        }
        ),
        n
    }
    defaultQueryOptions(e) {
        if (e._defaulted)
            return e;
        const t = {
            ...T(this, hn).queries,
            ...this.getQueryDefaults(e.queryKey),
            ...e,
            _defaulted: !0
        };
        return t.queryHash || (t.queryHash = vc(t.queryKey, t)),
        t.refetchOnReconnect === void 0 && (t.refetchOnReconnect = t.networkMode !== "always"),
        t.throwOnError === void 0 && (t.throwOnError = !!t.suspense),
        !t.networkMode && t.persister && (t.networkMode = "offlineFirst"),
        t.enabled !== !0 && t.queryFn === mv && (t.enabled = !1),
        t
    }
    defaultMutationOptions(e) {
        return e != null && e._defaulted ? e : {
            ...T(this, hn).mutations,
            ...(e == null ? void 0 : e.mutationKey) && this.getMutationDefaults(e.mutationKey),
            ...e,
            _defaulted: !0
        }
    }
    clear() {
        T(this, fe).clear(),
        T(this, pn).clear()
    }
}
,
fe = new WeakMap,
pn = new WeakMap,
hn = new WeakMap,
Dr = new WeakMap,
zr = new WeakMap,
mn = new WeakMap,
Fr = new WeakMap,
$r = new WeakMap,
jf), eS = g.createContext(void 0), tS = ({client: e, children: t}) => (g.useEffect( () => (e.mount(),
() => {
    e.unmount()
}
), [e]),
m.jsx(eS.Provider, {
    value: e,
    children: t
}));
/**
 * @remix-run/router v1.19.2
 *
 * Copyright (c) Remix Software Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE.md file in the root directory of this source tree.
 *
 * @license MIT
 */
function ti() {
    return ti = Object.assign ? Object.assign.bind() : function(e) {
        for (var t = 1; t < arguments.length; t++) {
            var n = arguments[t];
            for (var r in n)
                Object.prototype.hasOwnProperty.call(n, r) && (e[r] = n[r])
        }
        return e
    }
    ,
    ti.apply(this, arguments)
}
var yn;
(function(e) {
    e.Pop = "POP",
    e.Push = "PUSH",
    e.Replace = "REPLACE"
}
)(yn || (yn = {}));
const vf = "popstate";
function nS(e) {
    e === void 0 && (e = {});
    function t(r, o) {
        let {pathname: i, search: s, hash: l} = r.location;
        return ru("", {
            pathname: i,
            search: s,
            hash: l
        }, o.state && o.state.usr || null, o.state && o.state.key || "default")
    }
    function n(r, o) {
        return typeof o == "string" ? o : Os(o)
    }
    return oS(t, n, null, e)
}
function ge(e, t) {
    if (e === !1 || e === null || typeof e > "u")
        throw new Error(t)
}
function Sv(e, t) {
    if (!e) {
        typeof console < "u" && console.warn(t);
        try {
            throw new Error(t)
        } catch {}
    }
}
function rS() {
    return Math.random().toString(36).substr(2, 8)
}
function gf(e, t) {
    return {
        usr: e.state,
        key: e.key,
        idx: t
    }
}
function ru(e, t, n, r) {
    return n === void 0 && (n = null),
    ti({
        pathname: typeof e == "string" ? e : e.pathname,
        search: "",
        hash: ""
    }, typeof t == "string" ? ro(t) : t, {
        state: n,
        key: t && t.key || r || rS()
    })
}
function Os(e) {
    let {pathname: t="/", search: n="", hash: r=""} = e;
    return n && n !== "?" && (t += n.charAt(0) === "?" ? n : "?" + n),
    r && r !== "#" && (t += r.charAt(0) === "#" ? r : "#" + r),
    t
}
function ro(e) {
    let t = {};
    if (e) {
        let n = e.indexOf("#");
        n >= 0 && (t.hash = e.substr(n),
        e = e.substr(0, n));
        let r = e.indexOf("?");
        r >= 0 && (t.search = e.substr(r),
        e = e.substr(0, r)),
        e && (t.pathname = e)
    }
    return t
}
function oS(e, t, n, r) {
    r === void 0 && (r = {});
    let {window: o=document.defaultView, v5Compat: i=!1} = r
      , s = o.history
      , l = yn.Pop
      , a = null
      , u = d();
    u == null && (u = 0,
    s.replaceState(ti({}, s.state, {
        idx: u
    }), ""));
    function d() {
        return (s.state || {
            idx: null
        }).idx
    }
    function f() {
        l = yn.Pop;
        let E = d()
          , h = E == null ? null : E - u;
        u = E,
        a && a({
            action: l,
            location: x.location,
            delta: h
        })
    }
    function c(E, h) {
        l = yn.Push;
        let p = ru(x.location, E, h);
        u = d() + 1;
        let v = gf(p, u)
          , S = x.createHref(p);
        try {
            s.pushState(v, "", S)
        } catch (C) {
            if (C instanceof DOMException && C.name === "DataCloneError")
                throw C;
            o.location.assign(S)
        }
        i && a && a({
            action: l,
            location: x.location,
            delta: 1
        })
    }
    function y(E, h) {
        l = yn.Replace;
        let p = ru(x.location, E, h);
        u = d();
        let v = gf(p, u)
          , S = x.createHref(p);
        s.replaceState(v, "", S),
        i && a && a({
            action: l,
            location: x.location,
            delta: 0
        })
    }
    function w(E) {
        let h = o.location.origin !== "null" ? o.location.origin : o.location.href
          , p = typeof E == "string" ? E : Os(E);
        return p = p.replace(/ $/, "%20"),
        ge(h, "No window.location.(origin|href) available to create URL for href: " + p),
        new URL(p,h)
    }
    let x = {
        get action() {
            return l
        },
        get location() {
            return e(o, s)
        },
        listen(E) {
            if (a)
                throw new Error("A history only accepts one active listener");
            return o.addEventListener(vf, f),
            a = E,
            () => {
                o.removeEventListener(vf, f),
                a = null
            }
        },
        createHref(E) {
            return t(o, E)
        },
        createURL: w,
        encodeLocation(E) {
            let h = w(E);
            return {
                pathname: h.pathname,
                search: h.search,
                hash: h.hash
            }
        },
        push: c,
        replace: y,
        go(E) {
            return s.go(E)
        }
    };
    return x
}
var yf;
(function(e) {
    e.data = "data",
    e.deferred = "deferred",
    e.redirect = "redirect",
    e.error = "error"
}
)(yf || (yf = {}));
function iS(e, t, n) {
    return n === void 0 && (n = "/"),
    sS(e, t, n, !1)
}
function sS(e, t, n, r) {
    let o = typeof t == "string" ? ro(t) : t
      , i = gc(o.pathname || "/", n);
    if (i == null)
        return null;
    let s = Cv(e);
    lS(s);
    let l = null;
    for (let a = 0; l == null && a < s.length; ++a) {
        let u = yS(i);
        l = vS(s[a], u, r)
    }
    return l
}
function Cv(e, t, n, r) {
    t === void 0 && (t = []),
    n === void 0 && (n = []),
    r === void 0 && (r = "");
    let o = (i, s, l) => {
        let a = {
            relativePath: l === void 0 ? i.path || "" : l,
            caseSensitive: i.caseSensitive === !0,
            childrenIndex: s,
            route: i
        };
        a.relativePath.startsWith("/") && (ge(a.relativePath.startsWith(r), 'Absolute route path "' + a.relativePath + '" nested under path ' + ('"' + r + '" is not valid. An absolute child route path ') + "must start with the combined path of all its parent routes."),
        a.relativePath = a.relativePath.slice(r.length));
        let u = Nn([r, a.relativePath])
          , d = n.concat(a);
        i.children && i.children.length > 0 && (ge(i.index !== !0, "Index routes must not have child routes. Please remove " + ('all child routes from route path "' + u + '".')),
        Cv(i.children, t, d, u)),
        !(i.path == null && !i.index) && t.push({
            path: u,
            score: hS(u, i.index),
            routesMeta: d
        })
    }
    ;
    return e.forEach( (i, s) => {
        var l;
        if (i.path === "" || !((l = i.path) != null && l.includes("?")))
            o(i, s);
        else
            for (let a of bv(i.path))
                o(i, s, a)
    }
    ),
    t
}
function bv(e) {
    let t = e.split("/");
    if (t.length === 0)
        return [];
    let[n,...r] = t
      , o = n.endsWith("?")
      , i = n.replace(/\?$/, "");
    if (r.length === 0)
        return o ? [i, ""] : [i];
    let s = bv(r.join("/"))
      , l = [];
    return l.push(...s.map(a => a === "" ? i : [i, a].join("/"))),
    o && l.push(...s),
    l.map(a => e.startsWith("/") && a === "" ? "/" : a)
}
function lS(e) {
    e.sort( (t, n) => t.score !== n.score ? n.score - t.score : mS(t.routesMeta.map(r => r.childrenIndex), n.routesMeta.map(r => r.childrenIndex)))
}
const aS = /^:[\w-]+$/
  , uS = 3
  , cS = 2
  , dS = 1
  , fS = 10
  , pS = -2
  , xf = e => e === "*";
function hS(e, t) {
    let n = e.split("/")
      , r = n.length;
    return n.some(xf) && (r += pS),
    t && (r += cS),
    n.filter(o => !xf(o)).reduce( (o, i) => o + (aS.test(i) ? uS : i === "" ? dS : fS), r)
}
function mS(e, t) {
    return e.length === t.length && e.slice(0, -1).every( (r, o) => r === t[o]) ? e[e.length - 1] - t[t.length - 1] : 0
}
function vS(e, t, n) {
    let {routesMeta: r} = e
      , o = {}
      , i = "/"
      , s = [];
    for (let l = 0; l < r.length; ++l) {
        let a = r[l]
          , u = l === r.length - 1
          , d = i === "/" ? t : t.slice(i.length) || "/"
          , f = wf({
            path: a.relativePath,
            caseSensitive: a.caseSensitive,
            end: u
        }, d)
          , c = a.route;
        if (!f && u && n && !r[r.length - 1].route.index && (f = wf({
            path: a.relativePath,
            caseSensitive: a.caseSensitive,
            end: !1
        }, d)),
        !f)
            return null;
        Object.assign(o, f.params),
        s.push({
            params: o,
            pathname: Nn([i, f.pathname]),
            pathnameBase: SS(Nn([i, f.pathnameBase])),
            route: c
        }),
        f.pathnameBase !== "/" && (i = Nn([i, f.pathnameBase]))
    }
    return s
}
function wf(e, t) {
    typeof e == "string" && (e = {
        path: e,
        caseSensitive: !1,
        end: !0
    });
    let[n,r] = gS(e.path, e.caseSensitive, e.end)
      , o = t.match(n);
    if (!o)
        return null;
    let i = o[0]
      , s = i.replace(/(.)\/+$/, "$1")
      , l = o.slice(1);
    return {
        params: r.reduce( (u, d, f) => {
            let {paramName: c, isOptional: y} = d;
            if (c === "*") {
                let x = l[f] || "";
                s = i.slice(0, i.length - x.length).replace(/(.)\/+$/, "$1")
            }
            const w = l[f];
            return y && !w ? u[c] = void 0 : u[c] = (w || "").replace(/%2F/g, "/"),
            u
        }
        , {}),
        pathname: i,
        pathnameBase: s,
        pattern: e
    }
}
function gS(e, t, n) {
    t === void 0 && (t = !1),
    n === void 0 && (n = !0),
    Sv(e === "*" || !e.endsWith("*") || e.endsWith("/*"), 'Route path "' + e + '" will be treated as if it were ' + ('"' + e.replace(/\*$/, "/*") + '" because the `*` character must ') + "always follow a `/` in the pattern. To get rid of this warning, " + ('please change the route path to "' + e.replace(/\*$/, "/*") + '".'));
    let r = []
      , o = "^" + e.replace(/\/*\*?$/, "").replace(/^\/*/, "/").replace(/[\\.*+^${}|()[\]]/g, "\\$&").replace(/\/:([\w-]+)(\?)?/g, (s, l, a) => (r.push({
        paramName: l,
        isOptional: a != null
    }),
    a ? "/?([^\\/]+)?" : "/([^\\/]+)"));
    return e.endsWith("*") ? (r.push({
        paramName: "*"
    }),
    o += e === "*" || e === "/*" ? "(.*)$" : "(?:\\/(.+)|\\/*)$") : n ? o += "\\/*$" : e !== "" && e !== "/" && (o += "(?:(?=\\/|$))"),
    [new RegExp(o,t ? void 0 : "i"), r]
}
function yS(e) {
    try {
        return e.split("/").map(t => decodeURIComponent(t).replace(/\//g, "%2F")).join("/")
    } catch (t) {
        return Sv(!1, 'The URL path "' + e + '" could not be decoded because it is is a malformed URL segment. This is probably due to a bad percent ' + ("encoding (" + t + ").")),
        e
    }
}
function gc(e, t) {
    if (t === "/")
        return e;
    if (!e.toLowerCase().startsWith(t.toLowerCase()))
        return null;
    let n = t.endsWith("/") ? t.length - 1 : t.length
      , r = e.charAt(n);
    return r && r !== "/" ? null : e.slice(n) || "/"
}
function xS(e, t) {
    t === void 0 && (t = "/");
    let {pathname: n, search: r="", hash: o=""} = typeof e == "string" ? ro(e) : e;
    return {
        pathname: n ? n.startsWith("/") ? n : wS(n, t) : t,
        search: CS(r),
        hash: bS(o)
    }
}
function wS(e, t) {
    let n = t.replace(/\/+$/, "").split("/");
    return e.split("/").forEach(o => {
        o === ".." ? n.length > 1 && n.pop() : o !== "." && n.push(o)
    }
    ),
    n.length > 1 ? n.join("/") : "/"
}
function Xl(e, t, n, r) {
    return "Cannot include a '" + e + "' character in a manually specified " + ("`to." + t + "` field [" + JSON.stringify(r) + "].  Please separate it out to the ") + ("`to." + n + "` field. Alternatively you may provide the full path as ") + 'a string in <Link to="..."> and the router will parse it for you.'
}
function ES(e) {
    return e.filter( (t, n) => n === 0 || t.route.path && t.route.path.length > 0)
}
function kv(e, t) {
    let n = ES(e);
    return t ? n.map( (r, o) => o === n.length - 1 ? r.pathname : r.pathnameBase) : n.map(r => r.pathnameBase)
}
function Pv(e, t, n, r) {
    r === void 0 && (r = !1);
    let o;
    typeof e == "string" ? o = ro(e) : (o = ti({}, e),
    ge(!o.pathname || !o.pathname.includes("?"), Xl("?", "pathname", "search", o)),
    ge(!o.pathname || !o.pathname.includes("#"), Xl("#", "pathname", "hash", o)),
    ge(!o.search || !o.search.includes("#"), Xl("#", "search", "hash", o)));
    let i = e === "" || o.pathname === "", s = i ? "/" : o.pathname, l;
    if (s == null)
        l = n;
    else {
        let f = t.length - 1;
        if (!r && s.startsWith("..")) {
            let c = s.split("/");
            for (; c[0] === ".."; )
                c.shift(),
                f -= 1;
            o.pathname = c.join("/")
        }
        l = f >= 0 ? t[f] : "/"
    }
    let a = xS(o, l)
      , u = s && s !== "/" && s.endsWith("/")
      , d = (i || s === ".") && n.endsWith("/");
    return !a.pathname.endsWith("/") && (u || d) && (a.pathname += "/"),
    a
}
const Nn = e => e.join("/").replace(/\/\/+/g, "/")
  , SS = e => e.replace(/\/+$/, "").replace(/^\/*/, "/")
  , CS = e => !e || e === "?" ? "" : e.startsWith("?") ? e : "?" + e
  , bS = e => !e || e === "#" ? "" : e.startsWith("#") ? e : "#" + e;
function kS(e) {
    return e != null && typeof e.status == "number" && typeof e.statusText == "string" && typeof e.internal == "boolean" && "data"in e
}
const Nv = ["post", "put", "patch", "delete"];
new Set(Nv);
const PS = ["get", ...Nv];
new Set(PS);
/**
 * React Router v6.26.2
 *
 * Copyright (c) Remix Software Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE.md file in the root directory of this source tree.
 *
 * @license MIT
 */
function ni() {
    return ni = Object.assign ? Object.assign.bind() : function(e) {
        for (var t = 1; t < arguments.length; t++) {
            var n = arguments[t];
            for (var r in n)
                Object.prototype.hasOwnProperty.call(n, r) && (e[r] = n[r])
        }
        return e
    }
    ,
    ni.apply(this, arguments)
}
const yc = g.createContext(null)
  , NS = g.createContext(null)
  , ir = g.createContext(null)
  , al = g.createContext(null)
  , sr = g.createContext({
    outlet: null,
    matches: [],
    isDataRoute: !1
})
  , Tv = g.createContext(null);
function TS(e, t) {
    let {relative: n} = t === void 0 ? {} : t;
    fi() || ge(!1);
    let {basename: r, navigator: o} = g.useContext(ir)
      , {hash: i, pathname: s, search: l} = Av(e, {
        relative: n
    })
      , a = s;
    return r !== "/" && (a = s === "/" ? r : Nn([r, s])),
    o.createHref({
        pathname: a,
        search: l,
        hash: i
    })
}
function fi() {
    return g.useContext(al) != null
}
function ul() {
    return fi() || ge(!1),
    g.useContext(al).location
}
function Rv(e) {
    g.useContext(ir).static || g.useLayoutEffect(e)
}
function RS() {
    let {isDataRoute: e} = g.useContext(sr);
    return e ? BS() : AS()
}
function AS() {
    fi() || ge(!1);
    let e = g.useContext(yc)
      , {basename: t, future: n, navigator: r} = g.useContext(ir)
      , {matches: o} = g.useContext(sr)
      , {pathname: i} = ul()
      , s = JSON.stringify(kv(o, n.v7_relativeSplatPath))
      , l = g.useRef(!1);
    return Rv( () => {
        l.current = !0
    }
    ),
    g.useCallback(function(u, d) {
        if (d === void 0 && (d = {}),
        !l.current)
            return;
        if (typeof u == "number") {
            r.go(u);
            return
        }
        let f = Pv(u, JSON.parse(s), i, d.relative === "path");
        e == null && t !== "/" && (f.pathname = f.pathname === "/" ? t : Nn([t, f.pathname])),
        (d.replace ? r.replace : r.push)(f, d.state, d)
    }, [t, r, s, i, e])
}
function Av(e, t) {
    let {relative: n} = t === void 0 ? {} : t
      , {future: r} = g.useContext(ir)
      , {matches: o} = g.useContext(sr)
      , {pathname: i} = ul()
      , s = JSON.stringify(kv(o, r.v7_relativeSplatPath));
    return g.useMemo( () => Pv(e, JSON.parse(s), i, n === "path"), [e, s, i, n])
}
function jS(e, t) {
    return OS(e, t)
}
function OS(e, t, n, r) {
    fi() || ge(!1);
    let {navigator: o} = g.useContext(ir)
      , {matches: i} = g.useContext(sr)
      , s = i[i.length - 1]
      , l = s ? s.params : {};
    s && s.pathname;
    let a = s ? s.pathnameBase : "/";
    s && s.route;
    let u = ul(), d;
    if (t) {
        var f;
        let E = typeof t == "string" ? ro(t) : t;
        a === "/" || (f = E.pathname) != null && f.startsWith(a) || ge(!1),
        d = E
    } else
        d = u;
    let c = d.pathname || "/"
      , y = c;
    if (a !== "/") {
        let E = a.replace(/^\//, "").split("/");
        y = "/" + c.replace(/^\//, "").split("/").slice(E.length).join("/")
    }
    let w = iS(e, {
        pathname: y
    })
      , x = DS(w && w.map(E => Object.assign({}, E, {
        params: Object.assign({}, l, E.params),
        pathname: Nn([a, o.encodeLocation ? o.encodeLocation(E.pathname).pathname : E.pathname]),
        pathnameBase: E.pathnameBase === "/" ? a : Nn([a, o.encodeLocation ? o.encodeLocation(E.pathnameBase).pathname : E.pathnameBase])
    })), i, n, r);
    return t && x ? g.createElement(al.Provider, {
        value: {
            location: ni({
                pathname: "/",
                search: "",
                hash: "",
                state: null,
                key: "default"
            }, d),
            navigationType: yn.Pop
        }
    }, x) : x
}
function _S() {
    let e = US()
      , t = kS(e) ? e.status + " " + e.statusText : e instanceof Error ? e.message : JSON.stringify(e)
      , n = e instanceof Error ? e.stack : null
      , o = {
        padding: "0.5rem",
        backgroundColor: "rgba(200,200,200, 0.5)"
    };
    return g.createElement(g.Fragment, null, g.createElement("h2", null, "Unexpected Application Error!"), g.createElement("h3", {
        style: {
            fontStyle: "italic"
        }
    }, t), n ? g.createElement("pre", {
        style: o
    }, n) : null, null)
}
const LS = g.createElement(_S, null);
class MS extends g.Component {
    constructor(t) {
        super(t),
        this.state = {
            location: t.location,
            revalidation: t.revalidation,
            error: t.error
        }
    }
    static getDerivedStateFromError(t) {
        return {
            error: t
        }
    }
    static getDerivedStateFromProps(t, n) {
        return n.location !== t.location || n.revalidation !== "idle" && t.revalidation === "idle" ? {
            error: t.error,
            location: t.location,
            revalidation: t.revalidation
        } : {
            error: t.error !== void 0 ? t.error : n.error,
            location: n.location,
            revalidation: t.revalidation || n.revalidation
        }
    }
    componentDidCatch(t, n) {
        console.error("React Router caught the following error during render", t, n)
    }
    render() {
        return this.state.error !== void 0 ? g.createElement(sr.Provider, {
            value: this.props.routeContext
        }, g.createElement(Tv.Provider, {
            value: this.state.error,
            children: this.props.component
        })) : this.props.children
    }
}
function IS(e) {
    let {routeContext: t, match: n, children: r} = e
      , o = g.useContext(yc);
    return o && o.static && o.staticContext && (n.route.errorElement || n.route.ErrorBoundary) && (o.staticContext._deepestRenderedBoundaryId = n.route.id),
    g.createElement(sr.Provider, {
        value: t
    }, r)
}
function DS(e, t, n, r) {
    var o;
    if (t === void 0 && (t = []),
    n === void 0 && (n = null),
    r === void 0 && (r = null),
    e == null) {
        var i;
        if (!n)
            return null;
        if (n.errors)
            e = n.matches;
        else if ((i = r) != null && i.v7_partialHydration && t.length === 0 && !n.initialized && n.matches.length > 0)
            e = n.matches;
        else
            return null
    }
    let s = e
      , l = (o = n) == null ? void 0 : o.errors;
    if (l != null) {
        let d = s.findIndex(f => f.route.id && (l == null ? void 0 : l[f.route.id]) !== void 0);
        d >= 0 || ge(!1),
        s = s.slice(0, Math.min(s.length, d + 1))
    }
    let a = !1
      , u = -1;
    if (n && r && r.v7_partialHydration)
        for (let d = 0; d < s.length; d++) {
            let f = s[d];
            if ((f.route.HydrateFallback || f.route.hydrateFallbackElement) && (u = d),
            f.route.id) {
                let {loaderData: c, errors: y} = n
                  , w = f.route.loader && c[f.route.id] === void 0 && (!y || y[f.route.id] === void 0);
                if (f.route.lazy || w) {
                    a = !0,
                    u >= 0 ? s = s.slice(0, u + 1) : s = [s[0]];
                    break
                }
            }
        }
    return s.reduceRight( (d, f, c) => {
        let y, w = !1, x = null, E = null;
        n && (y = l && f.route.id ? l[f.route.id] : void 0,
        x = f.route.errorElement || LS,
        a && (u < 0 && c === 0 ? (w = !0,
        E = null) : u === c && (w = !0,
        E = f.route.hydrateFallbackElement || null)));
        let h = t.concat(s.slice(0, c + 1))
          , p = () => {
            let v;
            return y ? v = x : w ? v = E : f.route.Component ? v = g.createElement(f.route.Component, null) : f.route.element ? v = f.route.element : v = d,
            g.createElement(IS, {
                match: f,
                routeContext: {
                    outlet: d,
                    matches: h,
                    isDataRoute: n != null
                },
                children: v
            })
        }
        ;
        return n && (f.route.ErrorBoundary || f.route.errorElement || c === 0) ? g.createElement(MS, {
            location: n.location,
            revalidation: n.revalidation,
            component: x,
            error: y,
            children: p(),
            routeContext: {
                outlet: null,
                matches: h,
                isDataRoute: !0
            }
        }) : p()
    }
    , null)
}
var jv = function(e) {
    return e.UseBlocker = "useBlocker",
    e.UseRevalidator = "useRevalidator",
    e.UseNavigateStable = "useNavigate",
    e
}(jv || {})
  , _s = function(e) {
    return e.UseBlocker = "useBlocker",
    e.UseLoaderData = "useLoaderData",
    e.UseActionData = "useActionData",
    e.UseRouteError = "useRouteError",
    e.UseNavigation = "useNavigation",
    e.UseRouteLoaderData = "useRouteLoaderData",
    e.UseMatches = "useMatches",
    e.UseRevalidator = "useRevalidator",
    e.UseNavigateStable = "useNavigate",
    e.UseRouteId = "useRouteId",
    e
}(_s || {});
function zS(e) {
    let t = g.useContext(yc);
    return t || ge(!1),
    t
}
function FS(e) {
    let t = g.useContext(NS);
    return t || ge(!1),
    t
}
function $S(e) {
    let t = g.useContext(sr);
    return t || ge(!1),
    t
}
function Ov(e) {
    let t = $S()
      , n = t.matches[t.matches.length - 1];
    return n.route.id || ge(!1),
    n.route.id
}
function US() {
    var e;
    let t = g.useContext(Tv)
      , n = FS(_s.UseRouteError)
      , r = Ov(_s.UseRouteError);
    return t !== void 0 ? t : (e = n.errors) == null ? void 0 : e[r]
}
function BS() {
    let {router: e} = zS(jv.UseNavigateStable)
      , t = Ov(_s.UseNavigateStable)
      , n = g.useRef(!1);
    return Rv( () => {
        n.current = !0
    }
    ),
    g.useCallback(function(o, i) {
        i === void 0 && (i = {}),
        n.current && (typeof o == "number" ? e.navigate(o) : e.navigate(o, ni({
            fromRouteId: t
        }, i)))
    }, [e, t])
}
function $n(e) {
    ge(!1)
}
function WS(e) {
    let {basename: t="/", children: n=null, location: r, navigationType: o=yn.Pop, navigator: i, static: s=!1, future: l} = e;
    fi() && ge(!1);
    let a = t.replace(/^\/*/, "/")
      , u = g.useMemo( () => ({
        basename: a,
        navigator: i,
        static: s,
        future: ni({
            v7_relativeSplatPath: !1
        }, l)
    }), [a, l, i, s]);
    typeof r == "string" && (r = ro(r));
    let {pathname: d="/", search: f="", hash: c="", state: y=null, key: w="default"} = r
      , x = g.useMemo( () => {
        let E = gc(d, a);
        return E == null ? null : {
            location: {
                pathname: E,
                search: f,
                hash: c,
                state: y,
                key: w
            },
            navigationType: o
        }
    }
    , [a, d, f, c, y, w, o]);
    return x == null ? null : g.createElement(ir.Provider, {
        value: u
    }, g.createElement(al.Provider, {
        children: n,
        value: x
    }))
}
function VS(e) {
    let {children: t, location: n} = e;
    return jS(ou(t), n)
}
new Promise( () => {}
);
function ou(e, t) {
    t === void 0 && (t = []);
    let n = [];
    return g.Children.forEach(e, (r, o) => {
        if (!g.isValidElement(r))
            return;
        let i = [...t, o];
        if (r.type === g.Fragment) {
            n.push.apply(n, ou(r.props.children, i));
            return
        }
        r.type !== $n && ge(!1),
        !r.props.index || !r.props.children || ge(!1);
        let s = {
            id: r.props.id || i.join("-"),
            caseSensitive: r.props.caseSensitive,
            element: r.props.element,
            Component: r.props.Component,
            index: r.props.index,
            path: r.props.path,
            loader: r.props.loader,
            action: r.props.action,
            errorElement: r.props.errorElement,
            ErrorBoundary: r.props.ErrorBoundary,
            hasErrorBoundary: r.props.ErrorBoundary != null || r.props.errorElement != null,
            shouldRevalidate: r.props.shouldRevalidate,
            handle: r.props.handle,
            lazy: r.props.lazy
        };
        r.props.children && (s.children = ou(r.props.children, i)),
        n.push(s)
    }
    ),
    n
}
/**
 * React Router DOM v6.26.2
 *
 * Copyright (c) Remix Software Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE.md file in the root directory of this source tree.
 *
 * @license MIT
 */
function iu() {
    return iu = Object.assign ? Object.assign.bind() : function(e) {
        for (var t = 1; t < arguments.length; t++) {
            var n = arguments[t];
            for (var r in n)
                Object.prototype.hasOwnProperty.call(n, r) && (e[r] = n[r])
        }
        return e
    }
    ,
    iu.apply(this, arguments)
}
function HS(e, t) {
    if (e == null)
        return {};
    var n = {}, r = Object.keys(e), o, i;
    for (i = 0; i < r.length; i++)
        o = r[i],
        !(t.indexOf(o) >= 0) && (n[o] = e[o]);
    return n
}
function QS(e) {
    return !!(e.metaKey || e.altKey || e.ctrlKey || e.shiftKey)
}
function KS(e, t) {
    return e.button === 0 && (!t || t === "_self") && !QS(e)
}
const GS = ["onClick", "relative", "reloadDocument", "replace", "state", "target", "to", "preventScrollReset", "unstable_viewTransition"]
  , YS = "6";
try {
    window.__reactRouterVersion = YS
} catch {}
const XS = "startTransition"
  , Ef = Wf[XS];
function qS(e) {
    let {basename: t, children: n, future: r, window: o} = e
      , i = g.useRef();
    i.current == null && (i.current = nS({
        window: o,
        v5Compat: !0
    }));
    let s = i.current
      , [l,a] = g.useState({
        action: s.action,
        location: s.location
    })
      , {v7_startTransition: u} = r || {}
      , d = g.useCallback(f => {
        u && Ef ? Ef( () => a(f)) : a(f)
    }
    , [a, u]);
    return g.useLayoutEffect( () => s.listen(d), [s, d]),
    g.createElement(WS, {
        basename: t,
        children: n,
        location: l.location,
        navigationType: l.action,
        navigator: s,
        future: r
    })
}
const ZS = typeof window < "u" && typeof window.document < "u" && typeof window.document.createElement < "u"
  , JS = /^(?:[a-z][a-z0-9+.-]*:|\/\/)/i
  , Un = g.forwardRef(function(t, n) {
    let {onClick: r, relative: o, reloadDocument: i, replace: s, state: l, target: a, to: u, preventScrollReset: d, unstable_viewTransition: f} = t, c = HS(t, GS), {basename: y} = g.useContext(ir), w, x = !1;
    if (typeof u == "string" && JS.test(u) && (w = u,
    ZS))
        try {
            let v = new URL(window.location.href)
              , S = u.startsWith("//") ? new URL(v.protocol + u) : new URL(u)
              , C = gc(S.pathname, y);
            S.origin === v.origin && C != null ? u = C + S.search + S.hash : x = !0
        } catch {}
    let E = TS(u, {
        relative: o
    })
      , h = eC(u, {
        replace: s,
        state: l,
        target: a,
        preventScrollReset: d,
        relative: o,
        unstable_viewTransition: f
    });
    function p(v) {
        r && r(v),
        v.defaultPrevented || h(v)
    }
    return g.createElement("a", iu({}, c, {
        href: w || E,
        onClick: x || i ? r : p,
        ref: n,
        target: a
    }))
});
var Sf;
(function(e) {
    e.UseScrollRestoration = "useScrollRestoration",
    e.UseSubmit = "useSubmit",
    e.UseSubmitFetcher = "useSubmitFetcher",
    e.UseFetcher = "useFetcher",
    e.useViewTransitionState = "useViewTransitionState"
}
)(Sf || (Sf = {}));
var Cf;
(function(e) {
    e.UseFetcher = "useFetcher",
    e.UseFetchers = "useFetchers",
    e.UseScrollRestoration = "useScrollRestoration"
}
)(Cf || (Cf = {}));
function eC(e, t) {
    let {target: n, replace: r, state: o, preventScrollReset: i, relative: s, unstable_viewTransition: l} = t === void 0 ? {} : t
      , a = RS()
      , u = ul()
      , d = Av(e, {
        relative: s
    });
    return g.useCallback(f => {
        if (KS(f, n)) {
            f.preventDefault();
            let c = r !== void 0 ? r : Os(u) === Os(d);
            a(e, {
                replace: c,
                state: o,
                preventScrollReset: i,
                relative: s,
                unstable_viewTransition: l
            })
        }
    }
    , [u, a, d, r, o, n, e, i, s, l])
}
const tC = bm("inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0", {
    variants: {
        variant: {
            default: "bg-primary text-primary-foreground hover:bg-primary/90",
            destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/90",
            outline: "border border-input bg-background hover:bg-accent hover:text-accent-foreground",
            secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/80",
            ghost: "hover:bg-accent hover:text-accent-foreground",
            link: "text-primary underline-offset-4 hover:underline"
        },
        size: {
            default: "h-10 px-4 py-2",
            sm: "h-9 rounded-md px-3",
            lg: "h-11 rounded-md px-8",
            icon: "h-10 w-10"
        }
    },
    defaultVariants: {
        variant: "default",
        size: "default"
    }
})
  , pi = g.forwardRef( ({className: e, variant: t, size: n, asChild: r=!1, ...o}, i) => {
    const s = r ? Xo : "button";
    return m.jsx(s, {
        className: Ct(tC({
            variant: t,
            size: n,
            className: e
        })),
        ref: i,
        ...o
    })
}
);
pi.displayName = "Button";
const nC = () => m.jsx("section", {
    className: "py-16 px-4 bg-primary",
    children: m.jsxs("div", {
        className: "container mx-auto text-center",
        children: [m.jsx("h2", {
            className: "text-3xl md:text-4xl font-bold text-white mb-6",
            children: "Ready to Make the Right Choice?"
        }), m.jsx("p", {
            className: "text-white/90 text-xl mb-8 max-w-2xl mx-auto",
            children: "Get personalized recommendations and exclusive offers today. Our experts are ready to help you make the best decision."
        }), m.jsxs(pi, {
            onClick: () => window.open("https://wa.me/your_number", "_blank"),
            className: "bg-white text-primary hover:bg-white/90 px-8 py-6 text-lg flex items-center gap-2 mx-auto",
            children: [m.jsx(sc, {
                size: 24
            }), "Start Free Consultation"]
        })]
    })
});
var xc = "Collapsible"
  , [rC,_v] = ci(xc)
  , [oC,wc] = rC(xc)
  , Lv = g.forwardRef( (e, t) => {
    const {__scopeCollapsible: n, open: r, defaultOpen: o, disabled: i, onOpenChange: s, ...l} = e
      , [a=!1,u] = qs({
        prop: r,
        defaultProp: o,
        onChange: s
    });
    return m.jsx(oC, {
        scope: n,
        disabled: i,
        contentId: Fm(),
        open: a,
        onOpenToggle: g.useCallback( () => u(d => !d), [u]),
        children: m.jsx(me.div, {
            "data-state": Sc(a),
            "data-disabled": i ? "" : void 0,
            ...l,
            ref: t
        })
    })
}
);
Lv.displayName = xc;
var Mv = "CollapsibleTrigger"
  , Iv = g.forwardRef( (e, t) => {
    const {__scopeCollapsible: n, ...r} = e
      , o = wc(Mv, n);
    return m.jsx(me.button, {
        type: "button",
        "aria-controls": o.contentId,
        "aria-expanded": o.open || !1,
        "data-state": Sc(o.open),
        "data-disabled": o.disabled ? "" : void 0,
        disabled: o.disabled,
        ...r,
        ref: t,
        onClick: le(e.onClick, o.onOpenToggle)
    })
}
);
Iv.displayName = Mv;
var Ec = "CollapsibleContent"
  , Dv = g.forwardRef( (e, t) => {
    const {forceMount: n, ...r} = e
      , o = wc(Ec, e.__scopeCollapsible);
    return m.jsx(nc, {
        present: n || o.open,
        children: ({present: i}) => m.jsx(iC, {
            ...r,
            ref: t,
            present: i
        })
    })
}
);
Dv.displayName = Ec;
var iC = g.forwardRef( (e, t) => {
    const {__scopeCollapsible: n, present: r, children: o, ...i} = e
      , s = wc(Ec, n)
      , [l,a] = g.useState(r)
      , u = g.useRef(null)
      , d = Oe(t, u)
      , f = g.useRef(0)
      , c = f.current
      , y = g.useRef(0)
      , w = y.current
      , x = s.open || l
      , E = g.useRef(x)
      , h = g.useRef();
    return g.useEffect( () => {
        const p = requestAnimationFrame( () => E.current = !1);
        return () => cancelAnimationFrame(p)
    }
    , []),
    xt( () => {
        const p = u.current;
        if (p) {
            h.current = h.current || {
                transitionDuration: p.style.transitionDuration,
                animationName: p.style.animationName
            },
            p.style.transitionDuration = "0s",
            p.style.animationName = "none";
            const v = p.getBoundingClientRect();
            f.current = v.height,
            y.current = v.width,
            E.current || (p.style.transitionDuration = h.current.transitionDuration,
            p.style.animationName = h.current.animationName),
            a(r)
        }
    }
    , [s.open, r]),
    m.jsx(me.div, {
        "data-state": Sc(s.open),
        "data-disabled": s.disabled ? "" : void 0,
        id: s.contentId,
        hidden: !x,
        ...i,
        ref: d,
        style: {
            "--radix-collapsible-content-height": c ? `${c}px` : void 0,
            "--radix-collapsible-content-width": w ? `${w}px` : void 0,
            ...e.style
        },
        children: x && o
    })
}
);
function Sc(e) {
    return e ? "open" : "closed"
}
var sC = Lv
  , lC = Iv
  , aC = Dv
  , uC = g.createContext(void 0);
function cC(e) {
    const t = g.useContext(uC);
    return e || t || "ltr"
}
var Xt = "Accordion"
  , dC = ["Home", "End", "ArrowDown", "ArrowUp", "ArrowLeft", "ArrowRight"]
  , [Cc,fC,pC] = Xh(Xt)
  , [cl,WC] = ci(Xt, [pC, _v])
  , bc = _v()
  , zv = R.forwardRef( (e, t) => {
    const {type: n, ...r} = e
      , o = r
      , i = r;
    return m.jsx(Cc.Provider, {
        scope: e.__scopeAccordion,
        children: n === "multiple" ? m.jsx(gC, {
            ...i,
            ref: t
        }) : m.jsx(vC, {
            ...o,
            ref: t
        })
    })
}
);
zv.displayName = Xt;
var [Fv,hC] = cl(Xt)
  , [$v,mC] = cl(Xt, {
    collapsible: !1
})
  , vC = R.forwardRef( (e, t) => {
    const {value: n, defaultValue: r, onValueChange: o= () => {}
    , collapsible: i=!1, ...s} = e
      , [l,a] = qs({
        prop: n,
        defaultProp: r,
        onChange: o
    });
    return m.jsx(Fv, {
        scope: e.__scopeAccordion,
        value: l ? [l] : [],
        onItemOpen: a,
        onItemClose: R.useCallback( () => i && a(""), [i, a]),
        children: m.jsx($v, {
            scope: e.__scopeAccordion,
            collapsible: i,
            children: m.jsx(Uv, {
                ...s,
                ref: t
            })
        })
    })
}
)
  , gC = R.forwardRef( (e, t) => {
    const {value: n, defaultValue: r, onValueChange: o= () => {}
    , ...i} = e
      , [s=[],l] = qs({
        prop: n,
        defaultProp: r,
        onChange: o
    })
      , a = R.useCallback(d => l( (f=[]) => [...f, d]), [l])
      , u = R.useCallback(d => l( (f=[]) => f.filter(c => c !== d)), [l]);
    return m.jsx(Fv, {
        scope: e.__scopeAccordion,
        value: s,
        onItemOpen: a,
        onItemClose: u,
        children: m.jsx($v, {
            scope: e.__scopeAccordion,
            collapsible: !0,
            children: m.jsx(Uv, {
                ...i,
                ref: t
            })
        })
    })
}
)
  , [yC,dl] = cl(Xt)
  , Uv = R.forwardRef( (e, t) => {
    const {__scopeAccordion: n, disabled: r, dir: o, orientation: i="vertical", ...s} = e
      , l = R.useRef(null)
      , a = Oe(l, t)
      , u = fC(n)
      , f = cC(o) === "ltr"
      , c = le(e.onKeyDown, y => {
        var N;
        if (!dC.includes(y.key))
            return;
        const w = y.target
          , x = u().filter(_ => {
            var O;
            return !((O = _.ref.current) != null && O.disabled)
        }
        )
          , E = x.findIndex(_ => _.ref.current === w)
          , h = x.length;
        if (E === -1)
            return;
        y.preventDefault();
        let p = E;
        const v = 0
          , S = h - 1
          , C = () => {
            p = E + 1,
            p > S && (p = v)
        }
          , P = () => {
            p = E - 1,
            p < v && (p = S)
        }
        ;
        switch (y.key) {
        case "Home":
            p = v;
            break;
        case "End":
            p = S;
            break;
        case "ArrowRight":
            i === "horizontal" && (f ? C() : P());
            break;
        case "ArrowDown":
            i === "vertical" && C();
            break;
        case "ArrowLeft":
            i === "horizontal" && (f ? P() : C());
            break;
        case "ArrowUp":
            i === "vertical" && P();
            break
        }
        const b = p % h;
        (N = x[b].ref.current) == null || N.focus()
    }
    );
    return m.jsx(yC, {
        scope: n,
        disabled: r,
        direction: o,
        orientation: i,
        children: m.jsx(Cc.Slot, {
            scope: n,
            children: m.jsx(me.div, {
                ...s,
                "data-orientation": i,
                ref: a,
                onKeyDown: r ? void 0 : c
            })
        })
    })
}
)
  , Ls = "AccordionItem"
  , [xC,kc] = cl(Ls)
  , Bv = R.forwardRef( (e, t) => {
    const {__scopeAccordion: n, value: r, ...o} = e
      , i = dl(Ls, n)
      , s = hC(Ls, n)
      , l = bc(n)
      , a = Fm()
      , u = r && s.value.includes(r) || !1
      , d = i.disabled || e.disabled;
    return m.jsx(xC, {
        scope: n,
        open: u,
        disabled: d,
        triggerId: a,
        children: m.jsx(sC, {
            "data-orientation": i.orientation,
            "data-state": Gv(u),
            ...l,
            ...o,
            ref: t,
            disabled: d,
            open: u,
            onOpenChange: f => {
                f ? s.onItemOpen(r) : s.onItemClose(r)
            }
        })
    })
}
);
Bv.displayName = Ls;
var Wv = "AccordionHeader"
  , Vv = R.forwardRef( (e, t) => {
    const {__scopeAccordion: n, ...r} = e
      , o = dl(Xt, n)
      , i = kc(Wv, n);
    return m.jsx(me.h3, {
        "data-orientation": o.orientation,
        "data-state": Gv(i.open),
        "data-disabled": i.disabled ? "" : void 0,
        ...r,
        ref: t
    })
}
);
Vv.displayName = Wv;
var su = "AccordionTrigger"
  , Hv = R.forwardRef( (e, t) => {
    const {__scopeAccordion: n, ...r} = e
      , o = dl(Xt, n)
      , i = kc(su, n)
      , s = mC(su, n)
      , l = bc(n);
    return m.jsx(Cc.ItemSlot, {
        scope: n,
        children: m.jsx(lC, {
            "aria-disabled": i.open && !s.collapsible || void 0,
            "data-orientation": o.orientation,
            id: i.triggerId,
            ...l,
            ...r,
            ref: t
        })
    })
}
);
Hv.displayName = su;
var Qv = "AccordionContent"
  , Kv = R.forwardRef( (e, t) => {
    const {__scopeAccordion: n, ...r} = e
      , o = dl(Xt, n)
      , i = kc(Qv, n)
      , s = bc(n);
    return m.jsx(aC, {
        role: "region",
        "aria-labelledby": i.triggerId,
        "data-orientation": o.orientation,
        ...s,
        ...r,
        ref: t,
        style: {
            "--radix-accordion-content-height": "var(--radix-collapsible-content-height)",
            "--radix-accordion-content-width": "var(--radix-collapsible-content-width)",
            ...e.style
        }
    })
}
);
Kv.displayName = Qv;
function Gv(e) {
    return e ? "open" : "closed"
}
var wC = zv
  , EC = Bv
  , SC = Vv
  , Yv = Hv
  , Xv = Kv;
const CC = wC
  , qv = g.forwardRef( ({className: e, ...t}, n) => m.jsx(EC, {
    ref: n,
    className: Ct("border-b", e),
    ...t
}));
qv.displayName = "AccordionItem";
const Zv = g.forwardRef( ({className: e, children: t, ...n}, r) => m.jsx(SC, {
    className: "flex",
    children: m.jsxs(Yv, {
        ref: r,
        className: Ct("flex flex-1 items-center justify-between py-4 font-medium transition-all hover:underline [&[data-state=open]>svg]:rotate-180", e),
        ...n,
        children: [t, m.jsx(Rx, {
            className: "h-4 w-4 shrink-0 transition-transform duration-200"
        })]
    })
}));
Zv.displayName = Yv.displayName;
const Jv = g.forwardRef( ({className: e, children: t, ...n}, r) => m.jsx(Xv, {
    ref: r,
    className: "overflow-hidden text-sm transition-all data-[state=closed]:animate-accordion-up data-[state=open]:animate-accordion-down",
    ...n,
    children: m.jsx("div", {
        className: Ct("pb-4 pt-0", e),
        children: t
    })
}));
Jv.displayName = Xv.displayName;
const bC = [{
    question: "How does your service work?",
    answer: "We provide personalized recommendations through WhatsApp chat. Simply reach out to us with your requirements, and our experts will analyze your needs to suggest the best hosting, VPN, or tools along with exclusive offers."
}, {
    question: "Is your advice really free?",
    answer: "Yes, our consultation service is completely free! We earn through commissions from our partners when you make a purchase through our recommendations, but this doesn't affect our advice or the price you pay."
}, {
    question: "How do you ensure unbiased recommendations?",
    answer: "We partner with almost all major providers in the industry, which allows us to recommend truly the best solution for your needs rather than being limited to specific vendors."
}, {
    question: "What kind of exclusive offers do you provide?",
    answer: "Through our partnerships, we can offer special discounts, extended trial periods, and additional features that aren't available through direct purchases."
}]
  , kC = () => m.jsx("section", {
    className: "py-16 px-4",
    children: m.jsxs("div", {
        className: "container mx-auto max-w-3xl",
        children: [m.jsx("h2", {
            className: "text-3xl md:text-4xl font-bold text-center mb-12",
            children: "Frequently Asked Questions"
        }), m.jsx(CC, {
            type: "single",
            collapsible: !0,
            className: "w-full",
            children: bC.map( (e, t) => m.jsxs(qv, {
                value: `item-${t}`,
                children: [m.jsx(Zv, {
                    className: "text-left",
                    children: e.question
                }), m.jsx(Jv, {
                    children: e.answer
                })]
            }, t))
        })]
    })
})
  , PC = [{
    icon: _x,
    title: "Unbiased Recommendations",
    description: "We partner with all major providers to give you truly independent advice"
}, {
    icon: Mx,
    title: "Exclusive Offers",
    description: "Get access to special deals and discounts through our partnerships"
}, {
    icon: jx,
    title: "Expert Guidance",
    description: "Benefit from our years of experience in the hosting and VPN industry"
}, {
    icon: Tx,
    title: "Free Consultation",
    description: "Personal WhatsApp consultation at no cost to you"
}]
  , NC = () => m.jsx("section", {
    className: "py-16 px-4 bg-gray-50",
    children: m.jsxs("div", {
        className: "container mx-auto",
        children: [m.jsx("h2", {
            className: "text-3xl md:text-4xl font-bold text-center mb-12",
            children: "Why Choose BestSuggest?"
        }), m.jsx("div", {
            className: "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8",
            children: PC.map( (e, t) => m.jsxs("div", {
                className: "bg-white p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow duration-300 animate-fade-up",
                style: {
                    animationDelay: `${t * 100}ms`
                },
                children: [m.jsx("div", {
                    className: "w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center mb-4",
                    children: m.jsx(e.icon, {
                        className: "w-6 h-6 text-primary"
                    })
                }), m.jsx("h3", {
                    className: "text-xl font-semibold mb-2",
                    children: e.title
                }), m.jsx("p", {
                    className: "text-gray-600",
                    children: e.description
                })]
            }, t))
        })]
    })
})
  , oo = () => m.jsx("footer", {
    className: "bg-gray-50 py-12 px-4",
    children: m.jsxs("div", {
        className: "container mx-auto",
        children: [m.jsxs("div", {
            className: "grid grid-cols-1 md:grid-cols-4 gap-8",
            children: [m.jsxs("div", {
                children: [m.jsx("h3", {
                    className: "text-lg font-semibold mb-4",
                    children: "BestSuggest"
                }), m.jsx("p", {
                    className: "text-gray-600",
                    children: "Your trusted advisor for hosting, VPN, and tools recommendations."
                })]
            }), m.jsxs("div", {
                children: [m.jsx("h3", {
                    className: "text-lg font-semibold mb-4",
                    children: "Services"
                }), m.jsxs("ul", {
                    className: "space-y-2",
                    children: [m.jsx("li", {
                        children: m.jsx(Un, {
                            to: "/hostinger-review",
                            className: "text-gray-600 hover:text-primary",
                            children: "Hostinger Review"
                        })
                    }), m.jsx("li", {
                        children: m.jsx(Un, {
                            to: "/gifts",
                            className: "text-gray-600 hover:text-primary",
                            children: "Our Gifts"
                        })
                    }), m.jsx("li", {
                        children: m.jsx(Un, {
                            to: "/",
                            className: "text-gray-600 hover:text-primary",
                            children: "Tools Selection"
                        })
                    })]
                })]
            }), m.jsxs("div", {
                children: [m.jsx("h3", {
                    className: "text-lg font-semibold mb-4",
                    children: "Company"
                }), m.jsxs("ul", {
                    className: "space-y-2",
                    children: [m.jsx("li", {
                        children: m.jsx(Un, {
                            to: "/about",
                            className: "text-gray-600 hover:text-primary",
                            children: "About Us"
                        })
                    }), m.jsx("li", {
                        children: m.jsx(Un, {
                            to: "/privacy",
                            className: "text-gray-600 hover:text-primary",
                            children: "Privacy Policy"
                        })
                    }), m.jsx("li", {
                        children: m.jsx(Un, {
                            to: "/terms",
                            className: "text-gray-600 hover:text-primary",
                            children: "Terms of Service"
                        })
                    })]
                })]
            }), m.jsxs("div", {
                children: [m.jsx("h3", {
                    className: "text-lg font-semibold mb-4",
                    children: "Contact"
                }), m.jsxs("ul", {
                    className: "space-y-2",
                    children: [m.jsx("li", {
                        children: m.jsx("a", {
                            href: "https://wa.me/919253029002",
                            className: "text-gray-600 hover:text-primary",
                            target: "_blank",
                            rel: "noopener noreferrer",
                            children: "WhatsApp: +91 9253029002"
                        })
                    }), m.jsx("li", {
                        children: m.jsx("a", {
                            href: "mailto:talk@bestsuggest.in",
                            className: "text-gray-600 hover:text-primary",
                            children: "Email: talk@bestsuggest.in"
                        })
                    })]
                })]
            })]
        }), m.jsx("div", {
            className: "border-t border-gray-200 mt-8 pt-8 text-center text-gray-600",
            children: m.jsxs("p", {
                children: ["© ", new Date().getFullYear(), " BestSuggest. All rights reserved."]
            })
        })]
    })
})
  , io = () => m.jsx("header", {
    className: "fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-200",
    children: m.jsxs("div", {
        className: "container mx-auto px-4 py-4 flex items-center justify-between",
        children: [m.jsx(Un, {
            to: "/",
            className: "text-2xl font-bold text-primary",
            children: "BestSuggest"
        }), m.jsxs(pi, {
            onClick: () => window.open("https://wa.me/919253029002", "_blank"),
            className: "bg-primary hover:bg-primary/90 text-white flex items-center gap-2",
            children: [m.jsx(sc, {
                size: 20
            }), m.jsx("span", {
                className: "hidden sm:inline",
                children: "Chat with Us"
            })]
        })]
    })
})
  , TC = () => m.jsx("section", {
    className: "pt-32 pb-16 px-4 animate-fade-down",
    children: m.jsxs("div", {
        className: "container mx-auto text-center",
        children: [m.jsx("div", {
            className: "inline-flex items-center bg-primary/10 rounded-full px-4 py-1.5 mb-6",
            children: m.jsx("span", {
                className: "text-primary text-sm font-medium",
                children: "Free Expert Advice"
            })
        }), m.jsx("h1", {
            className: "text-4xl md:text-5xl lg:text-6xl font-bold text-gray-900 mb-6 max-w-4xl mx-auto leading-tight",
            children: "Get Personalized Recommendations for Hosting, VPN & Tools"
        }), m.jsx("p", {
            className: "text-xl text-gray-600 mb-8 max-w-2xl mx-auto",
            children: "We help you make the right choice with unbiased advice and exclusive offers, customized to your needs."
        }), m.jsxs(pi, {
            onClick: () => window.open("https://wa.me/your_number", "_blank"),
            className: "bg-primary hover:bg-primary/90 text-white px-8 py-6 text-lg flex items-center gap-2 mx-auto",
            children: [m.jsx(sc, {
                size: 24
            }), "Start Free Consultation"]
        })]
    })
})
  , RC = () => m.jsxs("div", {
    className: "min-h-screen bg-white",
    children: [m.jsx(io, {}), m.jsxs("main", {
        children: [m.jsx(TC, {}), m.jsx(NC, {}), m.jsx(kC, {}), m.jsx(nC, {})]
    }), m.jsx(oo, {})]
})
  , AC = () => m.jsxs("div", {
    className: "min-h-screen bg-white",
    children: [m.jsx(io, {}), m.jsx("main", {
        className: "pt-20",
        children: m.jsx("div", {
            className: "container mx-auto px-4 py-12",
            children: m.jsxs("div", {
                className: "max-w-4xl mx-auto",
                children: [m.jsxs("div", {
                    className: "flex items-center gap-2 mb-4",
                    children: [m.jsx(vo, {
                        className: "text-yellow-400",
                        fill: "currentColor"
                    }), m.jsx(vo, {
                        className: "text-yellow-400",
                        fill: "currentColor"
                    }), m.jsx(vo, {
                        className: "text-yellow-400",
                        fill: "currentColor"
                    }), m.jsx(vo, {
                        className: "text-yellow-400",
                        fill: "currentColor"
                    }), m.jsx(vo, {
                        className: "text-yellow-400",
                        fill: "currentColor"
                    })]
                }), m.jsx("h1", {
                    className: "text-4xl font-bold mb-6",
                    children: "Hostinger Hosting Review"
                }), m.jsxs("div", {
                    className: "prose max-w-none",
                    children: [m.jsx("p", {
                        className: "text-lg mb-6",
                        children: "After thorough testing and analysis, we highly recommend Hostinger for its exceptional performance, reliability, and value for money. Here's why:"
                    }), m.jsxs("ul", {
                        className: "space-y-4 mb-8",
                        children: [m.jsx("li", {
                            children: "Lightning-fast loading speeds with LiteSpeed servers"
                        }), m.jsx("li", {
                            children: "99.9% uptime guarantee"
                        }), m.jsx("li", {
                            children: "User-friendly control panel"
                        }), m.jsx("li", {
                            children: "24/7 customer support"
                        }), m.jsx("li", {
                            children: "Affordable pricing plans"
                        })]
                    }), m.jsxs(pi, {
                        onClick: () => window.open("https://hostinger.in/", "_blank"),
                        className: "bg-primary hover:bg-primary/90 text-white flex items-center gap-2 text-lg px-8 py-6",
                        children: [m.jsx(Ox, {
                            size: 24
                        }), "Get Best Deal Now"]
                    })]
                })]
            })
        })
    }), m.jsx(oo, {})]
})
  , jC = () => m.jsxs("div", {
    className: "min-h-screen bg-white",
    children: [m.jsx(io, {}), m.jsx("main", {
        className: "pt-20",
        children: m.jsx("div", {
            className: "container mx-auto px-4 py-12",
            children: m.jsxs("div", {
                className: "max-w-4xl mx-auto",
                children: [m.jsxs("div", {
                    className: "flex items-center gap-2 mb-6",
                    children: [m.jsx(Ax, {
                        className: "text-primary",
                        size: 32
                    }), m.jsx("h1", {
                        className: "text-4xl font-bold",
                        children: "Exclusive Benefits"
                    })]
                }), m.jsxs("div", {
                    className: "grid gap-6",
                    children: [m.jsxs("div", {
                        className: "bg-white rounded-lg shadow-lg p-6",
                        children: [m.jsxs("h2", {
                            className: "text-2xl font-semibold mb-4 flex items-center gap-2",
                            children: [m.jsx(Nx, {
                                className: "text-primary"
                            }), "Unbeatable Savings"]
                        }), m.jsx("p", {
                            className: "text-gray-600",
                            children: "You will pay the least amount possible for your hosting needs."
                        })]
                    }), m.jsxs("div", {
                        className: "bg-white rounded-lg shadow-lg p-6",
                        children: [m.jsx("h2", {
                            className: "text-2xl font-semibold mb-4",
                            children: "Additional Free Domain"
                        }), m.jsx("p", {
                            className: "text-gray-600",
                            children: "Get an extra free domain name with your purchase."
                        })]
                    }), m.jsxs("div", {
                        className: "bg-white rounded-lg shadow-lg p-6",
                        children: [m.jsx("h2", {
                            className: "text-2xl font-semibold mb-4",
                            children: "Premium WordPress Theme/Plugins"
                        }), m.jsx("p", {
                            className: "text-gray-600",
                            children: "Access to premium WordPress themes and plugins."
                        })]
                    }), m.jsxs("div", {
                        className: "bg-white rounded-lg shadow-lg p-6",
                        children: [m.jsx("h2", {
                            className: "text-2xl font-semibold mb-4",
                            children: "Access on Demand"
                        }), m.jsx("p", {
                            className: "text-gray-600",
                            children: "Get instant access to all your resources when you need them."
                        })]
                    }), m.jsxs("div", {
                        className: "bg-white rounded-lg shadow-lg p-6",
                        children: [m.jsx("h2", {
                            className: "text-2xl font-semibold mb-4",
                            children: "Additional Benefits"
                        }), m.jsxs("ul", {
                            className: "list-disc list-inside space-y-2 text-gray-600",
                            children: [m.jsx("li", {
                                children: "Premium Tools (Private)"
                            }), m.jsx("li", {
                                children: "Exclusive Courses (Private)"
                            }), m.jsx("li", {
                                children: "CashBacks (Selected Hostings)"
                            }), m.jsx("li", {
                                children: "Gift Cards (Selected Hostings)"
                            }), m.jsx("li", {
                                children: "OTT Benefits (Private)"
                            })]
                        })]
                    })]
                })]
            })
        })
    }), m.jsx(oo, {})]
})
  , OC = () => m.jsxs("div", {
    className: "min-h-screen bg-white",
    children: [m.jsx(io, {}), m.jsxs("main", {
        className: "container mx-auto px-4 py-24",
        children: [m.jsx("h1", {
            className: "text-4xl font-bold mb-8",
            children: "About BestSuggest"
        }), m.jsxs("div", {
            className: "prose max-w-none",
            children: [m.jsx("p", {
                className: "text-lg mb-6",
                children: "BestSuggest is your trusted advisor for making informed decisions about hosting services, VPNs, and essential digital tools. We understand that choosing the right digital services can be overwhelming, which is why we're here to help."
            }), m.jsx("p", {
                className: "text-lg mb-6",
                children: "Our team of experts provides personalized recommendations based on your specific needs and requirements. We've partnered with leading service providers to ensure you get not just the best advice, but also exclusive deals and offers."
            }), m.jsx("p", {
                className: "text-lg",
                children: "What sets us apart is our commitment to providing unbiased, personalized advice through direct consultation. We're available on WhatsApp to discuss your needs and help you make the best choice for your digital journey."
            })]
        })]
    }), m.jsx(oo, {})]
})
  , _C = () => m.jsxs("div", {
    className: "min-h-screen bg-white",
    children: [m.jsx(io, {}), m.jsxs("main", {
        className: "container mx-auto px-4 py-24",
        children: [m.jsx("h1", {
            className: "text-4xl font-bold mb-8",
            children: "Privacy Policy"
        }), m.jsxs("div", {
            className: "prose max-w-none",
            children: [m.jsxs("p", {
                className: "text-lg mb-6",
                children: ["Last updated: ", new Date().toLocaleDateString()]
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "Information We Collect"
            }), m.jsx("p", {
                className: "mb-4",
                children: "We collect information you provide directly to us when you use our consultation services, including:"
            }), m.jsxs("ul", {
                className: "list-disc pl-6 mb-6",
                children: [m.jsx("li", {
                    children: "Contact information (name, email, phone number)"
                }), m.jsx("li", {
                    children: "Business requirements and preferences"
                }), m.jsx("li", {
                    children: "Communication history"
                })]
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "How We Use Your Information"
            }), m.jsx("p", {
                className: "mb-4",
                children: "We use the information we collect to:"
            }), m.jsxs("ul", {
                className: "list-disc pl-6 mb-6",
                children: [m.jsx("li", {
                    children: "Provide personalized recommendations"
                }), m.jsx("li", {
                    children: "Communicate with you about services"
                }), m.jsx("li", {
                    children: "Improve our services"
                }), m.jsx("li", {
                    children: "Send you relevant offers and updates"
                })]
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "Contact Us"
            }), m.jsxs("p", {
                children: ["If you have any questions about our Privacy Policy, please contact us at", " ", m.jsx("a", {
                    href: "mailto:talk@bestsuggest.in",
                    className: "text-primary hover:underline",
                    children: "talk@bestsuggest.in"
                })]
            })]
        })]
    }), m.jsx(oo, {})]
})
  , LC = () => m.jsxs("div", {
    className: "min-h-screen bg-white",
    children: [m.jsx(io, {}), m.jsxs("main", {
        className: "container mx-auto px-4 py-24",
        children: [m.jsx("h1", {
            className: "text-4xl font-bold mb-8",
            children: "Terms of Service"
        }), m.jsxs("div", {
            className: "prose max-w-none",
            children: [m.jsxs("p", {
                className: "text-lg mb-6",
                children: ["Last updated: ", new Date().toLocaleDateString()]
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "1. Services"
            }), m.jsx("p", {
                className: "mb-6",
                children: "BestSuggest provides consultation services for hosting, VPN, and digital tools selection. Our recommendations are based on our professional experience and partnerships with service providers."
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "2. Affiliate Relationships"
            }), m.jsx("p", {
                className: "mb-6",
                children: "We may earn commissions through affiliate partnerships when you purchase services through our recommendations. This does not affect the cost to you or influence our recommendations."
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "3. Gift Program"
            }), m.jsx("p", {
                className: "mb-6",
                children: "Our gift program is subject to availability and may vary based on the services purchased. Specific terms apply to each offer and will be communicated during consultation."
            }), m.jsx("h2", {
                className: "text-2xl font-semibold mt-8 mb-4",
                children: "4. Contact"
            }), m.jsxs("p", {
                children: ["For any questions about these terms, please contact us at", " ", m.jsx("a", {
                    href: "mailto:talk@bestsuggest.in",
                    className: "text-primary hover:underline",
                    children: "talk@bestsuggest.in"
                })]
            })]
        })]
    }), m.jsx(oo, {})]
})
  , MC = new JE
  , IC = () => m.jsx(tS, {
    client: MC,
    children: m.jsxs(jE, {
        children: [m.jsx(gw, {}), m.jsx(Qw, {}), m.jsx(qS, {
            children: m.jsxs(VS, {
                children: [m.jsx($n, {
                    path: "/",
                    element: m.jsx(RC, {})
                }), m.jsx($n, {
                    path: "/hostinger-review",
                    element: m.jsx(AC, {})
                }), m.jsx($n, {
                    path: "/gifts",
                    element: m.jsx(jC, {})
                }), m.jsx($n, {
                    path: "/about",
                    element: m.jsx(OC, {})
                }), m.jsx($n, {
                    path: "/privacy",
                    element: m.jsx(_C, {})
                }), m.jsx($n, {
                    path: "/terms",
                    element: m.jsx(LC, {})
                })]
            })
        })]
    })
});
Kh(document.getElementById("root")).render(m.jsx(IC, {}));
