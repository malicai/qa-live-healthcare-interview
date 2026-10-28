import zhCN from './zh-CN';
import enUS from './en-US';
import type { LocaleMessages } from './zh-CN';

export type Locale = 'zh-CN' | 'en-US';

export interface LocaleOption {
  value: Locale;
  label: string;
}

export type { LocaleMessages };

export const messages: Record<Locale, LocaleMessages> = {
  'zh-CN': zhCN,
  'en-US': enUS,
};

export const localeOptions: LocaleOption[] = [
  { value: 'zh-CN', label: zhCN.language.zh },
  { value: 'en-US', label: enUS.language.en },
];
