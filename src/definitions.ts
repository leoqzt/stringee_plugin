export interface QzStringeePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  getpermission(): Promise<{ value: string }>;
  getConfig(options: { value: string }): Promise<{ value: string }>;
  outgoingCall(options: { value: string }): Promise<{ value: string }>;
  answerCall(): Promise<{ value: string }>;
  endCall(): Promise<{ value: string }>;
}
