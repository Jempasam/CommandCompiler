package jempasam.textanalyzis.reader;

import java.util.Iterator;

public class IteratorBufferedReader<T> implements SReader<T>{
	Iterator<T> it;
	T[] buffer;
	int end;
	int cur;
	
	public IteratorBufferedReader(Iterator<T> it, T[] buffer) {
		this.it=it;
		this.buffer=buffer;
		end=0;
		cur=0;
	}
	
	@Override
	public boolean hasNext() {
		return end!=cur || it.hasNext();
	}
	
	@Override
	public T next() {
		if(end==cur) {
			cur++;
			if(cur>=buffer.length)cur=0;
			end=cur;
			buffer[cur]=it.next();
		}else {
			cur++;
			if(cur>=buffer.length)cur=0;
		}
		
		return buffer[cur];
	}
	
	public void backward() {
		int oldcur=cur;
		cur--;
		if(cur<0)cur=buffer.length-1;
		if(cur==end)cur=oldcur;
	}
	
	public void forward() {
		if(cur!=end) {
			cur++;
			if(cur>=buffer.length)cur=0;
		}
	}

	@Override
	public void frontward() {
		next();
	}
}
