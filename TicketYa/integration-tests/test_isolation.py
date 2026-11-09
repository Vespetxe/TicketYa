import json
import unittest
from unittest.mock import patch, MagicMock
from isolation import verify_isolation, DESTINATIONS

class IsolationTest(unittest.TestCase):
    def responses(self):
        result=[]
        for service in ['gateway','eventos','inventario','entradas','compras']:
            value={'service':service}
            if service!='gateway':value['database']='ticketya_e1_'+service
            value.update({name:f'http://127.0.0.1:{port}' for name,port in DESTINATIONS.get(service,{}).items()})
            result.append(value)
        return result

    def transport(self, values):
        response=MagicMock()
        response.__enter__.return_value.read.side_effect=[json.dumps(v).encode() for v in values]
        return response

    @patch.dict('os.environ',{},clear=True)
    @patch('urllib.request.urlopen')
    def test_missing_token_never_contacts_any_service(self,http):
        with self.assertRaises(RuntimeError):verify_isolation()
        http.assert_not_called()

    @patch.dict('os.environ',{'TICKETYA_TEST_TOKEN':'a'*32},clear=True)
    @patch('urllib.request.urlopen')
    def test_wrong_database_aborts_before_mutations(self,http):
        values=self.responses();values[1]['database']='desarrollo'
        http.return_value=self.transport(values)
        with self.assertRaises(RuntimeError):verify_isolation()
        self.assertEqual(2,http.call_count)

    @patch.dict('os.environ',{'TICKETYA_TEST_TOKEN':'a'*32},clear=True)
    @patch('urllib.request.urlopen')
    def test_each_mixed_or_missing_outbound_destination_aborts_before_first_post(self,http):
        for index,service in enumerate(['gateway','eventos','inventario','entradas','compras']):
            for destination in DESTINATIONS.get(service,{}):
                for bad in [None,'http://localhost:8082']:
                    with self.subTest(service=service,destination=destination,bad=bad):
                        values=self.responses()
                        if bad is None:values[index].pop(destination)
                        else:values[index][destination]=bad
                        http.reset_mock();http.return_value=self.transport(values)
                        with self.assertRaises(RuntimeError):verify_isolation()
                        self.assertEqual(index+1,http.call_count)
                        self.assertTrue(all(call.args[0].get_method()=='GET' for call in http.call_args_list))

    @patch.dict('os.environ',{'TICKETYA_TEST_TOKEN':'a'*32},clear=True)
    @patch('urllib.request.urlopen')
    def test_all_databases_and_outbound_destinations_are_required(self,http):
        http.return_value=self.transport(self.responses())
        verify_isolation()
        self.assertEqual(5,http.call_count)
