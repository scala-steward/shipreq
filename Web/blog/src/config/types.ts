export interface GoogleAnalytics {
  trackingId: string
  jsUrl     : string
  disabled? : boolean
}

export interface StatCounter {
  project  : number
  security : string
  https    : boolean
  jsUrl    : string
  disabled?: boolean
}
