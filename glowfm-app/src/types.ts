export type Station = {
  stationuuid: string;
  name: string;
  url: string;
  urlResolved: string;
  favicon: string;
  tags: string;
  language: string;
  codec: string;
  bitrate: number;
  lastCheckOk: boolean;
  clickCount: number;
  unreliable?: boolean;
};

export type PlaybackState = 'idle' | 'buffering' | 'playing' | 'error';

export type SignalStrength = 'excellent' | 'good' | 'weak';

export type LanguageOption = 'all' | 'english' | 'spanish' | 'french' | 'german' | 'other';

export type NowPlayingInfo = {
  artist?: string;
  title?: string;
  raw?: string;
};
