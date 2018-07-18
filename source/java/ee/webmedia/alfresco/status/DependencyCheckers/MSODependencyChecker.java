package ee.webmedia.alfresco.status.DependencyCheckers;

import java.io.*;
import java.net.*;
import javax.activation.DataSource; 
import javax.mail.internet.MimeMultipart;
import java.util.List; 


/**
 * 
 * @author viljar.tina
 *
 */
public class MSODependencyChecker extends DependencyChecker{

	
	/**
	 * 
	 */
	public MSODependencyChecker( String name, String uri, Boolean isFatal ) {
		super(name, uri, isFatal);
	}
	
	
	
	/**
	 * 
	 */
	public Boolean Test ( ) {
		try{
			URL url = new URL(this.Uri + "?singleWsdl");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty( "Content-Type", "text/xml;charset=UTF-8"); 
			conn.setRequestProperty( "SOAPAction", "http://webmedia.ee/mso/MsoPortBinding/convertToPdf");

			conn.setUseCaches(false);
			
			
			StringBuilder postData = new StringBuilder();
			postData.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mso=\"http://webmedia.ee/mso\">"
					+"<soapenv:Header/>"
					+"<soapenv:Body>"
					+"<mso:msoDocumentInput>"
					+"<mso:documentFile>UEsDBBQABgAIAAAAIQDfpNJsWgEAACAFAAATAAgCW0NvbnRlbnRfVHlwZXNdLnhtbCCiBAIooAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC0lMtuwjAQRfeV+g+Rt1Vi6KKqKgKLPpYtUukHGHsCVv2Sx7z+vhMCUVUBkQpsIiUz994zVsaD0dqabAkRtXcl6xc9loGTXmk3K9nX5C1/ZBkm4ZQw3kHJNoBsNLy9GUw2ATAjtcOSzVMKT5yjnIMVWPgAjiqVj1Ykeo0zHoT8FjPg973eA5feJXApT7UHGw5eoBILk7LXNX1uSCIYZNlz01hnlUyEYLQUiep86dSflHyXUJBy24NzHfCOGhg/mFBXjgfsdB90NFEryMYipndhqYuvfFRcebmwpCxO2xzg9FWlJbT62i1ELwGRztyaoq1Yod2e/ygHpo0BvDxF49sdDymR4BoAO+dOhBVMP69G8cu8E6Si3ImYGrg8RmvdCZFoA6F59s/m2NqciqTOcfQBaaPjP8ber2ytzmngADHp039dm0jWZ88H9W2gQB3I5tv7bfgDAAD//wMAUEsDBBQABgAIAAAAIQAekRq37wAAAE4CAAALAAgCX3JlbHMvLnJlbHMgogQCKKAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAArJLBasMwDEDvg/2D0b1R2sEYo04vY9DbGNkHCFtJTBPb2GrX/v082NgCXelhR8vS05PQenOcRnXglF3wGpZVDYq9Cdb5XsNb+7x4AJWFvKUxeNZw4gyb5vZm/cojSSnKg4tZFYrPGgaR+IiYzcAT5SpE9uWnC2kiKc/UYySzo55xVdf3mH4zoJkx1dZqSFt7B6o9Rb6GHbrOGX4KZj+xlzMtkI/C3rJdxFTqk7gyjWop9SwabDAvJZyRYqwKGvC80ep6o7+nxYmFLAmhCYkv+3xmXBJa/ueK5hk/Nu8hWbRf4W8bnF1B8wEAAP//AwBQSwMEFAAGAAgAAAAhANZks1H0AAAAMQMAABwACAF3b3JkL19yZWxzL2RvY3VtZW50LnhtbC5yZWxzIKIEASigAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAArJLLasMwEEX3hf6DmH0tO31QQuRsSiHb1v0ARR4/qCwJzfThv69ISevQYLrwcq6Yc8+ANtvPwYp3jNR7p6DIchDojK971yp4qR6v7kEQa1dr6x0qGJFgW15ebJ7Qak5L1PWBRKI4UtAxh7WUZDocNGU+oEsvjY+D5jTGVgZtXnWLcpXndzJOGVCeMMWuVhB39TWIagz4H7Zvmt7ggzdvAzo+UyE/cP+MzOk4SlgdW2QFkzBLRJDnRVZLitAfi2Myp1AsqsCjxanAYZ6rv12yntMu/rYfxu+wmHO4WdKh8Y4rvbcTj5/oKCFPPnr5BQAA//8DAFBLAwQUAAYACAAAACEAvRKk1UgCAADUBgAAEQAAAHdvcmQvZG9jdW1lbnQueG1spFVdb9sgFH2ftP9g8Z7abvqRWXGqtmmrPkyKmvV5IhjbKMBFQOJ1v34XO46zTarS5gnu17nnXAye3vxSMtpy6wTonKRnCYm4ZlAIXeXk9cfjaEIi56kuqATNc/LGHbmZff0ybbIC2EZx7SOE0C5rDMtJ7b3J4tixmivqzpRgFhyU/oyBiqEsBeNxA7aIz5M0aXfGAuPOYb97qrfUkR2c+h8NDNcYLMEq6tG0VayoXW/MCNEN9WIlpPBviJ1c9TCQk43V2Q5itCcUSrKO0G7pK+wxfbuS+W4CbcfYcokcQLtamEHGZ9EwWPcg2/dEbJXs8xqTXpx2BnNLG1wGwGPoF12Rkh3z9xHT5IgTCRD7imMo/N2zZ6Ko0EPjT43mYLjp5ccAzv8FMNVph/NkYWMGNHEa2rNe77HCzf4A1u6QD6W508gsa2rwBiqWPVcaLF1JZIRHFuHUo/BZkxm+OCso3sJqoibDF6t4yUmSPDwmtw+XpHctbHDe342vbwfnnJd0I/1BpIVZ2LDYbpFUV5i/pTInXI9elySeTeNdOB6yjy1pMj9bSVRCg8t3gU4FrMO7tfTUeiwXBfIKVDVVqPrnE9xRtg5QQ+6DLvaZXQ8Two4zv7DHTqPVXC1/YxAvVZp+C89kk9W4v5qMJ11HU32nAdED3v30Ir1usURV+8FcgfegBlvy8iBac1pw7HudTIJZAvgDs9r41mx1NBkD6dDrDGW8y2nd+G95siJolkLzhfAMWY6vevGd7nbbfRPx8Dua/QEAAP//AwBQSwMEFAAGAAgAAAAhAKpSJd8jBgAAixoAABUAAAB3b3JkL3RoZW1lL3RoZW1lMS54bWzsWU2LGzcYvhf6H8TcHX/N+GOJN9hjO2mzm4TsJiVHeUaeUawZGUneXRMCJTkWCqVp6aGB3noobQMJ9JL+mm1T2hTyF6rReGzJllnabGApWcNaH8/76tH7So80nstXThICjhDjmKYdp3qp4gCUBjTEadRx7hwOSy0HcAHTEBKaoo4zR9y5svvhB5fhjohRgoC0T/kO7DixENOdcpkHshnyS3SKUtk3piyBQlZZVA4ZPJZ+E1KuVSqNcgJx6oAUJtLtzfEYBwgcZi6d3cL5gMh/qeBZQ0DYQeYaGRYKG06q2Refc58wcARJx5HjhPT4EJ0IBxDIhezoOBX155R3L5eXRkRssdXshupvYbcwCCc1Zcei0dLQdT230V36VwAiNnGD5qAxaCz9KQAMAjnTnIuO9XrtXt9bYDVQXrT47jf79aqB1/zXN/BdL/sYeAXKi+4Gfjj0VzHUQHnRs8SkWfNdA69AebGxgW9Wun23aeAVKCY4nWygK16j7hezXULGlFyzwtueO2zWFvAVqqytrtw+FdvWWgLvUzaUAJVcKHAKxHyKxjCQOB8SPGIY7OEolgtvClPKZXOlVhlW6vJ/9nFVSUUE7iCoWedNAd9oyvgAHjA8FR3nY+nV0SBvXv745uVzcProxemjX04fPz599LPF6hpMI93q9fdf/P30U/DX8+9eP/nKjuc6/vefPvvt1y/tQKEDX3397I8Xz1598/mfPzyxwLsMjnT4IU4QBzfQMbhNEzkxywBoxP6dxWEMsW7RTSMOU5jZWNADERvoG3NIoAXXQ2YE7zIpEzbg1dl9g/BBzGYCW4DX48QA7lNKepRZ53Q9G0uPwiyN7IOzmY67DeGRbWx/Lb+D2VSud2xz6cfIoHmLyJTDCKVIgKyPThCymN3D2IjrPg4Y5XQswD0MehBbQ3KIR8ZqWhldw4nMy9xGUObbiM3+XdCjxOa+j45MpNwVkNhcImKE8SqcCZhYGcOE6Mg9KGIbyYM5C4yAcyEzHSFCwSBEnNtsbrK5Qfe6lBd72vfJPDGRTOCJDbkHKdWRfTrxY5hMrZxxGuvYj/hELlEIblFhJUHNHZLVZR5gujXddzEy0n323r4jldW+QLKeGbNtCUTN/TgnY4iU8/Kanic4PVPc12Tde7eyLoX01bdP7bp7IQW9y7B1R63L+Dbcunj7lIX44mt3H87SW0huFwv0vXS/l+7/vXRv28/nL9grjVaX+OKqrtwkW+/tY0zIgZgTtMeVunM5vXAoG1VFGS0fE6axLC6GM3ARg6oMGBWfYBEfxHAqh6mqESK+cB1xMKVcng+q2eo76yCzZJ+GeWu1WjyZSgMoVu3yfCna5Wkk8tZGc/UItnSvapF6VC4IZLb/hoQ2mEmibiHRLBrPIKFmdi4s2hYWrcz9Vhbqa5EVuf8AzH7U8NyckVxvkKAwy1NuX2T33DO9LZjmtGuW6bUzrueTaYOEttxMEtoyjGGI1pvPOdftVUoNelkoNmk0W+8i15mIrGkDSc0aOJZ7ru5JNwGcdpyxvBnKYjKV/nimm5BEaccJxCLQ/0VZpoyLPuRxDlNd+fwTLBADBCdyretpIOmKW7XWzOZ4Qcm1KxcvcupLTzIaj1EgtrSsqrIvd2LtfUtwVqEzSfogDo/BiMzYbSgD5TWrWQBDzMUymiFm2uJeRXFNrhZb0fjFbLVFIZnGcHGi6GKew1V5SUebh2K6PiuzvpjMKMqS9Nan7tlGWYcmmlsOkOzUtOvHuzvkNVYr3TdY5dK9rnXtQuu2nRJvfyBo1FaDGdQyxhZqq1aT2jleCLThlktz2xlx3qfB+qrNDojiXqlqG68m6Oi+XPl9eV2dEcEVVXQinxH84kflXAlUa6EuJwLMGO44Dype1/Vrnl+qtLxBya27lVLL69ZLXc+rVwdetdLv1R7KoIg4qXr52EP5PEPmizcvqn3j7UtSXLMvBTQpU3UPLitj9falWtv+9gVgGZkHjdqwXW/3GqV2vTssuf1eq9T2G71Sv+E3+8O+77Xaw4cOOFJgt1v33cagVWpUfb/kNioZ/Va71HRrta7b7LYGbvfhItZy5sV3EV7Fa/cfAAAA//8DAFBLAwQUAAYACAAAACEAC8/h2K0DAAC6CQAAEQAAAHdvcmQvc2V0dGluZ3MueG1stFZLb9s4EL4v0P9g6FzFkvxIoK1TOH5sU8TbReVe9kaJlE2EL5CUHXex/32HlBg5bVG4W/Rkar5585uh37x94mxwINpQKWZRepVEAyIqianYzaJP23V8Ew2MRQIjJgWZRSdiore3r357c8wNsRbUzABcCJPzahbtrVX5cGiqPeHIXElFBIC11BxZ+NS7IUf6sVFxJblClpaUUXsaZkkyjTo3chY1WuSdi5jTSksja+tMclnXtCLdT7DQl8RtTZayajgR1kccasIgBynMnioTvPH/6w3AfXBy+F4RB86C3jFNLij3KDV+trgkPWegtKyIMXBBnIUEqegDj79y9Bz7CmJ3JXpXYJ4m/nSe+eTHHGRfODDskkpa6IGWGumWJ10ZvMrvd0JqVDJgJZQzgIyiW6DlZyn54Jgroiu4G+B0lkVDB0BHZF1YZAnAO404cHEWVYwg0SpgUqOG2S0qCysVKB0QJHmd3LTw/qT2RHjG/A2zEPBxNmnxao80qizRhUIV9H0hhdWSBT0s/5R2AbzXcC2dhZ+C/lS0EwUWAnEo68WUbCQGyh/zRtPLO+8MfPQ0JPnNQBI2gKaYbF07C3tiZA3JF/QzmQv8vjGWgkdf+U9k8L0EoK8Q+QMQYHtSZE2QbaBNvyiYv4k1o2pDtZb6XmAgyi8LRuuaaAhAgXgboBfV8uj7/I4gDIv3J+MOz2kEaxybcPgopQ2qSbK4G13POxI49BJktU7mK48Mn33z3C26v3Q4OaIMeGuxQLzUFA02bhUOnUapH++oCHhJYLbJOVI0ZQDjuAUMR4ytYZICkLRyTI1aktqf2QbpXe+309DflMJUv3/25VYC0X9o2agWPWqkWgIElXQ87iypsA+UB7lpyiJYCdhGZ1Aj8IeD9n3q23PMLVykH6QH5AnhdYmNV6uOMEwX7rLJBinVcqbcpbOI0d3epu6aLXxheDH9R7nLOizzWNZi/gNVrjLQ7g69LAuyM71RkI162TjIxr1sEmSTXjYNsqmTwU4kmlHxCPQNRyevJWPySPC7Hv9K1DbB7JEiy3bzAr1kK+hWsRkccvIES5xgauGPiKKYoye4oySbOvNOm6GTbOwLXYc5ZfXSA0YWhcF5Yewp/kUu7kWoKNCxOPGyX+Sv28QZNTDsCna+lTpgv3ssnfjHwG6BxY9wsR9JfYcMwR2GZXWP3XvV2vwzH41ulvMsjReTu2k8vh4v45tpuopX2WQNksk4nWf/dlMY/nTd/gcAAP//AwBQSwMEFAAGAAgAAAAhAG97L+KqAQAA7QQAABIAAAB3b3JkL2ZvbnRUYWJsZS54bWzckl1r2zAUhu8H+w9G941lJ2k7U6f0KzAYuyjdD1AU2T5MH0ZHiZt/3yPZyWCl0NzsYjYI6X2lR0cv5+b21ehsrzyCszUrZpxlykq3BdvW7NfL+uKaZRiE3QrtrKrZQSG7XX39cjNUjbMBMzpvsTKyZl0IfZXnKDtlBM5cryyZjfNGBFr6NjfC/971F9KZXgTYgIZwyEvOL9mE8Z+huKYBqR6d3BllQzqfe6WJ6Cx20OORNnyGNji/7b2TCpHebPTIMwLsCVMs3oEMSO/QNWFGj5kqSig6XvA0M/oPYHkeoDwBjKy+t9Z5sdEUPlWSEYytpvSzobLCkPEgNGw8JKMX1qEqyNsLXTNe8jVf0hj/BZ/HkeVxo+yERxUhaeP93Sg3woA+HFUcAHE0egiyO+p74SEWNVoILRk73PCaPXHOy7v1mo1KQdVFZXF1PyklFTV+3yZlflJ4VGTipGUxcmTinPbQnfmYwLskXsAozH6qIXt2RtgPEin5JSWxpDxiMvOzEvGJe3YiT38ncnW9/CeJTL2R/YC2Cx92SOyL/7RDpgmu3gAAAP//AwBQSwMEFAAGAAgAAAAhAFtt/ZMJAQAA8QEAABQAAAB3b3JkL3dlYlNldHRpbmdzLnhtbJTRwUoDMRAG4LvgOyy5t9kWFVm6LYhUvIigPkCazrbBTCbMpK716R1rrUgv9ZZJMh8z/JPZO8bqDVgCpdaMhrWpIHlahrRqzcvzfHBtKikuLV2kBK3ZgpjZ9Pxs0jc9LJ6gFP0plSpJGvStWZeSG2vFrwGdDClD0seOGF3RklcWHb9u8sATZlfCIsRQtnZc11dmz/ApCnVd8HBLfoOQyq7fMkQVKck6ZPnR+lO0nniZmTyI6D4Yvz10IR2Y0cURhMEzCXVlqMvsJ9pR2j6qdyeMv8Dl/4DxAUDf3K8SsVtEjUAnqRQzU82AcgkYPmBOfMPUC7D9unYxUv/4cKeF/RPU9BMAAP//AwBQSwMEFAAGAAgAAAAhAEESt8NrAQAAxQIAABAACAFkb2NQcm9wcy9hcHAueG1sIKIEASigAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAnFJNS8QwEL0L/ofSu01XcJFlmkVWxINfsFXPIZm2wTQJSVbcf+90u9aqN3Oa9ybzePMSWH/0JnvHELWzVb4oyjxDK53Stq3y5/rm7DLPYhJWCeMsVvkeY77mpyfwFJzHkDTGjCRsrPIuJb9iLMoOexELalvqNC70IhEMLXNNoyVeO7nr0SZ2XpZLhh8JrUJ15ifBfFRcvaf/iionB3/xpd570uNQY++NSMgfhklTKJd6YBMLtUvC1LpHXhI9AXgSLUa+ADYW8OqCOuCxgE0ngpCJ8uNLYDMEV94bLUWiXPm9lsFF16Ts8WA2G6aBza8ALbBFuQs67QcPcwh32o4uxoJcBdEG4bujtQnBVgqDG1qdN8JEBPZNwMb1XliSY1NFem/x2dfuekjhOPKTnK34qlO39ULir2VnPGyJRUXuJwMTAbf0GMEM6jRrW1Rfd/42hvhexl/JFxdFSeeQ1xdHW0/fhX8CAAD//wMAUEsDBBQABgAIAAAAIQDdy1IecgEAAPkCAAARAAgBZG9jUHJvcHMvY29yZS54bWwgogQBKKAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACcksFOwzAMhu9IvEOVe5t0Q2hUbScG2olJSAyBuIXE20LbJEqylb09abt2VOzEzY4//3b+JJ1/V2VwAGOFkhmKI4ICkExxIbcZel0vwxkKrKOS01JJyNARLJrn11cp0wlTBp6N0mCcABt4JWkTpjO0c04nGFu2g4rayBPSFzfKVNT51GyxpqygW8ATQm5xBY5y6ihuBEM9KKKTJGeDpN6bshXgDEMJFUhncRzF+Mw6MJW92NBWfpGVcEcNF9G+ONDfVgxgXddRPW1Rv3+M31dPL+1VQyEbrxigPOUsccKVkKf4HPrI7j+/gLnueEh8zAxQp0x+X0JhveEmWJh9cRCyJftq43sBx1oZbr3GKPMYB8uM0M6/ZjdhdODpklq38s+7EcAXx0vD/kJNn4GDaP5IHrfEkKYnw7sFgQfeqKSzta+8TR8e10uUT0g8C8lNGN+tySyZkoSQj2bHUf9ZsDot8G/FXqCzafxZ8x8AAAD//wMAUEsDBBQABgAIAAAAIQBLmcEBIQsAAARwAAAPAAAAd29yZC9zdHlsZXMueG1svJ1dc9u6EYbvO9P/wNFVe+HI8mfiOc4Z27FrT+Mcn8hpriESklCDhMqP2O6vLwBSEuklKC649ZUtSvsAxLsviOWH9NvvL7EMfvE0Eyo5H00+7I8CnoQqEsnifPTj8Wbv4yjIcpZETKqEn49eeTb6/fNf//Lb81mWv0qeBRqQZGdxeD5a5vnqbDzOwiWPWfZBrXii35yrNGa5fpkuxjFLn4rVXqjiFcvFTEiRv44P9vdPRhUm7UNR87kI+RcVFjFPchs/TrnURJVkS7HK1rTnPrRnlUarVIU8y/ROx7LkxUwkG8zkCIBiEaYqU/P8g96ZqkcWpcMn+/a/WG4BxzjAwQYQh2d3i0SlbCb16OueBBo2+qyHP1LhFz5nhcwz8zJ9SKuX1Sv750YleRY8n7EsFOJRt6whsdC824skEyP9DmdZfpEJ1vrm0vzT+k6Y5bXNlyISo7FpMfuvfvMXk+ejg4P1livTg8Y2yZLFehvP966v6z3Rm5K9H1Ozaaa55yOW7k0vTOC42rHyb213V29f2YZXLBS2HTbPuc6sycm+gUphEvng+NP6xffCjC0rclU1YgHl3w12DEZcJ5xOv2npAv0un39V4ROPprl+43xk29Ibf9w9pEKlOtPPR59sm3rjlMfiVkQRT2ofTJYi4j+XPPmR8Wi7/c8bm63VhlAVif7/8HRis0Bm0fVLyFcm9/W7CTOafDMB0ny6ENvGbfh/1rBJpURb/JIzMwEEk7cI230U4sBEZLW9bWcWb/bdfgrV0OF7NXT0Xg0dv1dDJ+/V0Ol7NfTxvRqymP9nQyKJ+EtpRNgMoO7iONyI5jjMhuY4vITmOKyC5jicgOY4Eh3NceQxmuNIUwQnV6ErC2vJfujI9m7u7mOEH3f3IcGPu/sI4MfdPeH7cXfP737c3dO5H3f37O3H3T1Z47nlUiu40zZL8sEumyuVJyrnQc5fhtNYolm2KqLhmYMeT0l2kgBTzmzVgXgwLWT29e4MsSb1P57nppAL1DyYi0WR6mJ6aMd58otLXdYGLIo0jxCY8rxIHSPik9Mpn/OUJyGnTGw6qKkEg6SIZwS5uWILMhZPIuLhWxNJJoVNQuv6eWlMIgiSOmZhqoZ3TTGy+eGryIaPlYEEl4WUnIj1jSbFLGt4bWAxw0sDixleGVjM8MKgphnVEFU0opGqaEQDVtGIxq3MT6pxq2hE41bRiMatog0ft0eRSzvF11cdk/7n7q6kMuexB/djKhYJ0wuA4Yeb6pxp8MBStkjZahmYs9Lt2Po+Y9u5VNFr8EhxTNuQqNb1NkWu9F6LpBg+oA0albk2PCJ7bXhEBtvwhlvsXi+TzQLtlqaemRazvNW0ltTLtFMmi3JBO9xtLB+eYVsD3Ig0I7NBO5Ygg7+Z5ayRk2Lm2/ZyeMe2rOG2ejsrkXavQhL0UqrwiWYavn1d8VSXZU+DSTdKSvXMIzriNE9VmWt1yx9YSXpZ/jpeLVkmbK3UQPQ/1K+vgAf3bDV4hx4kEwmNbtd7MRMyoFtB3D7efw0e1cqUmWZgaICXKs9VTMaszgT+7Sef/Z2mgxe6CE5eifb2guj0kIVdCYKDTElSERFJLzNFIkiOoZb3T/46UyyNaGgPKS9vOsk5EXHK4lW56CDwlp4Xn/X8Q7Aasrx/sVSY80JUpnokgdVOG2bF7N88HD7VfVMByZmhP4rcnn+0S10bTYcbvkxo4IYvEaya+vBg8pdgZxu44TvbwFHt7JVkWSacl1C9eVS7u+ZR7+/w4q/iKanSeSHpBnANJBvBNZBsCJUs4iSj3GPLI9xhy6PeX8KUsTyCU3KW949URGRiWBiVEhZGJYOFUWlgYaQCDL9DpwYbfptODTb8Xp0SRrQEqMGo8oz08E90lacGo8ozC6PKMwujyjMLo8qzwy8Bn8/1IpjuEFNDUuVcDUl3oElyHq9UytJXIuS15AtGcIK0pD2kam6eRlBJeRM3AdKco5aEi+0SRyXyTz4j65phUfaL4Iwok1IponNr2wOOjWzeu7YrzD7JMbgLD5KFfKlkxFPHPrljdb08LR/LeNt9241epz2/isUyD6bLzdn+OuZkf2fkumBvhO1usG3MT9bPs7SF3fNIFPG6o/BhipPD/sE2oxvBR7uDtyuJRuRxz0jY5snuyO0quRF52jMStvmxZ6T1aSOyyw9fWPrUmginXfmzqfEcyXfalUWb4NZmuxJpE9mWgqddWdSwSnARhuZqAVSnn2fc8f3M447HuMhNwdjJTentKzeiy2Df+S9hjuyYSdO2t7l7Asz7dhHda+b8s1DlefvGBaf+D3Xd6YVTkvGglXPY/8JVY5Zxj2Pv6caN6D3vuBG9JyA3otdM5AxHTUluSu+5yY3oPUm5EejZCh4RcLMVjMfNVjDeZ7aCFJ/ZasAqwI3ovRxwI9BGhQi0UQesFNwIlFFBuJdRIQVtVIhAGxUi0EaFCzCcUWE8zqgw3seokOJjVEhBGxUi0EaFCLRRIQJtVIhAG9Vzbe8M9zIqpKCNChFoo0IE2qh2vTjAqDAeZ1QY72NUSPExKqSgjQoRaKNCBNqoEIE2KkSgjQoRKKOCcC+jQgraqBCBNipEoI1aPmrob1QYjzMqjPcxKqT4GBVS0EaFCLRRIQJtVIhAGxUi0EaFCJRRQbiXUSEFbVSIQBsVItBGtRcLBxgVxuOMCuN9jAopPkaFFLRRIQJtVIhAGxUi0EaFCLRRIQJlVBDuZVRIQRsVItBGhYiu/KwuUbpus5/gz3o679jvf+mq6tT3+qPcddRhf9S6V25W/2cRLpV6ClofPDy09UY/iJhJoewpasdl9TrX3hKBuvD5x1X3Ez51+sAvXaqehbDXTAH8qG8kOKdy1JXy9UhQ5B11ZXo9Eqw6j7pm33okOAwedU261pfrm1L04QgEd00zteCJI7xrtq6FwyHumqNrgXCEu2bmWiAc4K75uBZ4HJjJ+W30cc9xOtncXwoIXelYI5y6CV1pCbVaT8fQGH1FcxP6qucm9JXRTUDp6cTghXWj0Aq7UX5SQ5thpfY3qpuAlRoSvKQGGH+pIcpbaojykxpOjFipIQErtf/k7CZ4SQ0w/lJDlLfUEOUnNTyUYaWGBKzUkICVeuAB2YnxlxqivKWGKD+p4eIOKzUkYKWGBKzUkOAlNcD4Sw1R3lJDlJ/UoEpGSw0JWKkhASs1JHhJDTD+UkOUt9QQ1SW1PYvSkBqlcC0ctwirBeIOyLVA3ORcC/SolmrRntVSjeBZLUGt1prjqqW6aG5CX/XchL4yugkoPZ0YvLBuFFphN8pPaly11Ca1v1HdBKzUuGrJKTWuWuqUGlctdUqNq5bcUuOqpTapcdVSm9T+k7Ob4CU1rlrqlBpXLXVKjauW3FLjqqU2qXHVUpvUuGqpTeqBB2Qnxl9qXLXUKTWuWnJLjauW2qTGVUttUuOqpTapcdWSU2pctdQpNa5a6pQaVy25pcZVS21S46qlNqlx1VKb1LhqySk1rlrqlBpXLXVK7aiWxs+NH2AybPuDZPrD+euKm+/grj0wE5XfQVpdBLQfvIs2P5Rkgk1PguonqarNtsPVBcOyRRsImwqXuq2w+vYkR1PVt6BuHuOx34H6tmHHV6XajmyHYP3paki3l0LLzzUue3b2OzdD3tFnK0nnGJWquTr4qUrDXT3U/ZnJ8ke79D93SaQBz9UPVpU9jV5YidLvX3Ep71n5abVyf1TyeV6+O9m3D82/eX9Wfv+bMz61E4UTMG52pnxZ/XCYY7zLb4SvrmA7U9K4oWW47e0UQ0d627f1f9nn/wEAAP//AwBQSwECLQAUAAYACAAAACEA36TSbFoBAAAgBQAAEwAAAAAAAAAAAAAAAAAAAAAAW0NvbnRlbnRfVHlwZXNdLnhtbFBLAQItABQABgAIAAAAIQAekRq37wAAAE4CAAALAAAAAAAAAAAAAAAAAJMDAABfcmVscy8ucmVsc1BLAQItABQABgAIAAAAIQDWZLNR9AAAADEDAAAcAAAAAAAAAAAAAAAAALMGAAB3b3JkL19yZWxzL2RvY3VtZW50LnhtbC5yZWxzUEsBAi0AFAAGAAgAAAAhAL0SpNVIAgAA1AYAABEAAAAAAAAAAAAAAAAA6QgAAHdvcmQvZG9jdW1lbnQueG1sUEsBAi0AFAAGAAgAAAAhAKpSJd8jBgAAixoAABUAAAAAAAAAAAAAAAAAYAsAAHdvcmQvdGhlbWUvdGhlbWUxLnhtbFBLAQItABQABgAIAAAAIQALz+HYrQMAALoJAAARAAAAAAAAAAAAAAAAALYRAAB3b3JkL3NldHRpbmdzLnhtbFBLAQItABQABgAIAAAAIQBvey/iqgEAAO0EAAASAAAAAAAAAAAAAAAAAJIVAAB3b3JkL2ZvbnRUYWJsZS54bWxQSwECLQAUAAYACAAAACEAW239kwkBAADxAQAAFAAAAAAAAAAAAAAAAABsFwAAd29yZC93ZWJTZXR0aW5ncy54bWxQSwECLQAUAAYACAAAACEAQRK3w2sBAADFAgAAEAAAAAAAAAAAAAAAAACnGAAAZG9jUHJvcHMvYXBwLnhtbFBLAQItABQABgAIAAAAIQDdy1IecgEAAPkCAAARAAAAAAAAAAAAAAAAAEgbAABkb2NQcm9wcy9jb3JlLnhtbFBLAQItABQABgAIAAAAIQBLmcEBIQsAAARwAAAPAAAAAAAAAAAAAAAAAPEdAAB3b3JkL3N0eWxlcy54bWxQSwUGAAAAAAsACwDBAgAAPykAAAAA</mso:documentFile>"   
					+"<mso:fileEncoding>UTF-8</mso:fileEncoding>"
					+"<mso:fileType>application/vnd.openxmlformats-officedocument.wordprocessingml.document</mso:fileType>"
					+"</mso:msoDocumentInput>"
					+"</soapenv:Body>"
					+"</soapenv:Envelope>");


			//Send request ..
			conn.getOutputStream().write(postData.toString().getBytes("UTF-8"));

			
			// Get response ..
			Integer statusCode = conn.getResponseCode(); 
			String statusMesg = conn.getResponseMessage();
			Status = STATUS_NOK;
			if(statusCode.equals(200)){
				
				
				// TODO: pmts võiks ka tsekata, kas vastuse manusena ikkagi ka pdf tagasi saadetakse.
				//String ctype = conn.getHeaderField("Content-Type");            
				//MimeMultipart  mp = new MimeMultipart(new InputStreamDataSource(conn.getInputStream(), ctype));
				
				
				Status = STATUS_OK;
			    return true;
			}
									
			// ..
			StatusMsg = statusCode + " " + statusMesg;	
			
		    return false;
		}catch(Exception ex){
			StatusMsg = ex.getMessage();
			
			Status = STATUS_NOK;
		    return false;
		}
		
	}
		
		private class InputStreamDataSource implements DataSource { 
			 
		    protected final InputStream in; 
		 
		    protected final String ctype; 
		 
		    protected final String name; 
		 
		    public InputStreamDataSource(InputStream in, String ctype) { 
		        this(in, ctype, "MultipartRequest"); 
		    } 
		 
		    public InputStreamDataSource(InputStream in, String ctype, String name) { 
		        this.in = in; 
		        this.name = name; 
		        this.ctype = ctype; 
		    } 
		 
		    public OutputStream getOutputStream() throws IOException { 
		        throw new UnsupportedOperationException("data source is not writeable"); 
		    } 
		 
		    public String getName() { 
		        return name; 
		    } 
		 
		    public InputStream getInputStream() throws IOException { 
		        return in; 
		    } 
		 
		    public String getContentType() { 
		        return ctype; 
		    } 
		
		
		
    }

	
}