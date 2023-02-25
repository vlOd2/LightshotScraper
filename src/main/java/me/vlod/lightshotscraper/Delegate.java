package me.vlod.lightshotscraper;


public interface Delegate {
	public static final Delegate empty = new Delegate() {
		@Override
		public void call(Object... args) {
		}
	};
	public void call(Object... args);
}
