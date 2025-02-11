export interface QzStringeePlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
