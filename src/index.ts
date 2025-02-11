import { registerPlugin } from '@capacitor/core';

import type { QzStringeePlugin } from './definitions';

const QzStringee = registerPlugin<QzStringeePlugin>('QzStringee', {
  web: () => import('./web').then((m) => new m.QzStringeeWeb()),
});

export * from './definitions';
export { QzStringee };
