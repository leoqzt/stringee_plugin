import { WebPlugin } from '@capacitor/core';

import type { QzStringeePlugin } from './definitions';

export class QzStringeeWeb extends WebPlugin implements QzStringeePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
